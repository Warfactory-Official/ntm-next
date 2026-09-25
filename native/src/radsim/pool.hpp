// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include <array>
#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <mutex>
#include <thread>
#include <utility>
#include <vector>
#include <chrono>
#include <cstdlib>
#if defined(__x86_64__) || defined(__i386__)
#include <immintrin.h>
#endif

namespace hbm::radsim::pool {

    class SpinLock {
      public:
        void lock() noexcept {
            for (;;) {
                if (!flag_.exchange(true, std::memory_order_acquire))
                    return;
                while (flag_.load(std::memory_order_relaxed))
                    relax();
            }
        }

        void unlock() noexcept { flag_.store(false, std::memory_order_release); }

        static void relax() noexcept {
#if defined(__x86_64__) || defined(__i386__)
            _mm_pause();
#else
            std::this_thread::yield();
#endif
        }

      private:
        std::atomic<bool> flag_{false};
    };

    class Pool {
      public:
        static Pool &instance() {
            static Pool *p = new Pool();
            return *p;
        }

        void resize(int32_t threads) {
            std::unique_lock lock(park_mutex_);
            const int32_t want = threads < 1 ? 1 : threads;
            parallelism_.store(want, std::memory_order_relaxed);
            const int32_t workers_wanted = want - 1;
            while (worker_count_ < workers_wanted) {

                std::thread([this, index = worker_count_] { worker_loop(index); }).detach();
                ++worker_count_;
            }
        }

        [[nodiscard]] int32_t parallelism() const { return parallelism_.load(std::memory_order_relaxed); }

        void set_active_target(int32_t target) { active_target_.store(target, std::memory_order_relaxed); }

        static void relax() noexcept { cpu_relax(); }

        [[nodiscard]] static int32_t hardware_concurrency() {
            const unsigned hc = std::thread::hardware_concurrency();
            return hc == 0u ? 1 : static_cast<int32_t>(hc);
        }

        template <typename... Work> void invoke(Work &&...work) {
            static_assert(sizeof...(Work) >= 1);
            std::atomic pending{static_cast<int32_t>(sizeof...(Work)) - 1};
            fork_all(pending, std::forward<Work>(work)...);
            wait_helping(pending);
        }

        template <typename Fn> void invoke_n(int32_t n, Fn &fn) {
            if (n <= 1) {
                fn();
                return;
            }
            const int32_t tasks = (n > kMaxSlots ? kMaxSlots : n) - 1;
            std::atomic<int32_t> pending{tasks};

            FanNode<Fn> nodes[kMaxSlots];
            FanNode<Fn> root{this, &fn, &pending, nodes, 0, tasks};
            fan_spawn(root);
            fn();
            wait_helping(pending);
        }

      private:
        template <typename Fn> struct FanNode {
            Pool *pool;
            Fn *fn;
            std::atomic<int32_t> *pending;
            FanNode *nodes;
            int32_t lo;
            int32_t hi;
        };

        template <typename Fn> static void fan_run(void *arg) {
            auto *self = static_cast<FanNode<Fn> *>(arg);
            self->pool->fan_spawn(*self);
            (*self->fn)();
        }

        template <typename Fn> void fan_spawn(FanNode<Fn> &self) {
            while (self.lo < self.hi) {
                const int32_t mid = self.lo + (self.hi - self.lo) / 2;
                FanNode<Fn> &child = self.nodes[static_cast<size_t>(mid)];
                child = FanNode<Fn>{this, self.fn, self.pending, self.nodes, mid + 1, self.hi};
                push(Task{&fan_run<Fn>, static_cast<void *>(&child), self.pending});
                self.hi = mid;
            }
        }

        struct Task {
            void (*run)(void *);
            void *arg;
            std::atomic<int32_t> *pending;
        };

        Pool() = default;

        template <typename Last> void fork_all(std::atomic<int32_t> &, Last &&last) { std::forward<Last>(last)(); }

        template <typename First, typename... Rest>
        void fork_all(std::atomic<int32_t> &pending, First &&first, Rest &&...rest) {

            push(Task{&invoke_erased<std::remove_reference_t<First>>,
                      const_cast<void *>(static_cast<const void *>(&first)), &pending});
            fork_all(pending, std::forward<Rest>(rest)...);
        }

        template <typename F> static void invoke_erased(void *arg) { (*static_cast<F *>(arg))(); }

        bool try_take(Task &out) { return slot_pop_local(out) || slot_steal(out); }

        [[nodiscard]] bool work_visible() const { return any_slot_work(); }

        struct alignas(64) Slot {
            SpinLock lock;
            std::vector<Task> q;
            size_t head = 0u;

            std::atomic<int32_t> depth{0};
        };

        static constexpr int32_t kMaxSlots = 64;

        [[nodiscard]] int32_t live_slots() const {
            const int32_t n = slots_used_.load(std::memory_order_relaxed);
            return n > kMaxSlots ? kMaxSlots : n;
        }

        int32_t my_slot() {
            thread_local int32_t id = -1;
            if (id < 0)
                id = slots_used_.fetch_add(1, std::memory_order_relaxed) % kMaxSlots;
            return id;
        }

        static void slot_publish(Slot &sl) {
            sl.depth.store(static_cast<int32_t>(sl.q.size() - sl.head), std::memory_order_relaxed);
        }

        static void slot_compact(Slot &sl) {
            if (sl.head >= sl.q.size()) {
                sl.q.clear();
                sl.head = 0u;
            }
            slot_publish(sl);
        }

        void slot_push(Task task) {
            Slot &sl = slots_[static_cast<size_t>(my_slot())];
            std::lock_guard<SpinLock> g(sl.lock);
            sl.q.push_back(task);
            slot_publish(sl);
        }

        bool slot_pop_local(Task &out) {
            Slot &sl = slots_[static_cast<size_t>(my_slot())];
            if (sl.depth.load(std::memory_order_relaxed) == 0)
                return false;
            std::lock_guard<SpinLock> g(sl.lock);
            if (sl.head >= sl.q.size())
                return false;
            out = sl.q.back();
            sl.q.pop_back();
            slot_compact(sl);
            return true;
        }

        bool slot_steal(Task &out) {
            const int32_t self = my_slot();
            const int32_t n = live_slots();
            if (n <= 1)
                return false;

            thread_local int32_t cursor = 0;
            for (int32_t i = 0; i < n; i++) {
                const int32_t v = (self + 1 + cursor + i) % n;
                Slot &sl = slots_[static_cast<size_t>(v)];
                if (sl.depth.load(std::memory_order_relaxed) <= 0)
                    continue;
                {
                    std::lock_guard<SpinLock> g(sl.lock);
                    if (sl.head >= sl.q.size())
                        continue;
                    out = sl.q[sl.head++];
                    slot_compact(sl);
                }
                cursor = v;
                return true;
            }
            return false;
        }

        [[nodiscard]] bool any_slot_work() const {
            const int32_t n = live_slots();
            for (int32_t i = 0; i < n; i++) {
                if (slots_[static_cast<size_t>(i)].depth.load(std::memory_order_relaxed) > 0)
                    return true;
            }
            return false;
        }

        void push(Task task) {
            slot_push(task);

            if (parked_.load(std::memory_order_acquire) > 0) {

                {
                    std::lock_guard<std::mutex> g(park_mutex_);
                }
                park_cv_.notify_one();
            }
        }

        bool try_run_one() {
            Task task{};
            if (!work_visible() || !try_take(task)) {
                return false;
            }
            task.run(task.arg);
            task.pending->fetch_sub(1, std::memory_order_release);
            return true;
        }

        void wait_helping(std::atomic<int32_t> &pending) {
            for (int32_t spin = 0, pauses = kPauseSpins; pending.load(std::memory_order_acquire) > 0;) {
                if (try_run_one()) {
                    spin = 0;
                    continue;
                }

                if (spin < pauses)
                    cpu_relax();
                else
                    std::this_thread::yield();
                spin++;
            }
        }

        void worker_loop(int32_t index) {
            for (;;) {
                Task task{};
                bool got = false;
                const int32_t pauses = kPauseSpins;
                const int64_t budget = spin_budget_us();
                auto deadline = std::chrono::steady_clock::now() + std::chrono::microseconds(budget);
                for (int32_t spin = 0; !got; spin++) {
                    if (over_step_width(index)) {

                        if (slot_pop_local(task)) {
                            got = true;
                            break;
                        }
                        if ((spin % pauses) == pauses - 1) {

                            deadline = std::chrono::steady_clock::now() + std::chrono::microseconds(budget);
                            std::this_thread::yield();
                        } else {
                            cpu_relax();
                        }
                        continue;
                    }
                    if (!work_visible()) {
                        if ((spin % pauses) == pauses - 1) {
                            if (std::chrono::steady_clock::now() >= deadline)
                                break;
                            std::this_thread::yield();
                        } else {
                            cpu_relax();
                        }
                        continue;
                    }
                    got = try_take(task);
                }
                if (!got) {
                    std::unique_lock<std::mutex> lock(park_mutex_);

                    parked_.fetch_add(1, std::memory_order_release);
                    park_cv_.wait(lock, [this] { return work_visible(); });
                    parked_.fetch_sub(1, std::memory_order_relaxed);
                    continue;
                }
                task.run(task.arg);
                task.pending->fetch_sub(1, std::memory_order_release);
            }
        }

        static void cpu_relax() noexcept {
#if defined(__x86_64__) || defined(__i386__)
            _mm_pause();
#else
            std::this_thread::yield();
#endif
        }

        int64_t spin_budget_us() const {

            const int64_t threads = parallelism();
            const int64_t us = 16 * (threads < 1 ? 1 : threads);
            return us < 30 ? 30 : (us > 4000 ? 4000 : us);
        }

        static constexpr int32_t kPauseSpins = 256;

        [[nodiscard]] bool over_step_width(int32_t index) const {
            const int32_t target = active_target_.load(std::memory_order_relaxed);
            return target > 0 && index + 1 >= target;
        }

        std::atomic<int32_t> parked_{0};
        std::array<Slot, kMaxSlots> slots_{};
        std::atomic<int32_t> slots_used_{0};
        mutable std::mutex park_mutex_;
        std::condition_variable park_cv_;
        int32_t worker_count_ = 0;
        std::atomic<int32_t> parallelism_{1};
        std::atomic<int32_t> active_target_{0};
    };

    template <typename... Work> inline void parallel_invoke(Work &&...work) {
        Pool::instance().invoke(std::forward<Work>(work)...);
    }

    inline int32_t default_concurrency() { return Pool::hardware_concurrency(); }

}

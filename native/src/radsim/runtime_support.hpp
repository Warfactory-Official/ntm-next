// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include "radsim/world_state.hpp"

#include <algorithm>
#include <atomic>
#include <cstddef>
#include <cstdlib>
#include <memory>
#include <string_view>
#include <utility>

#include "radsim/pool.hpp"

namespace hbm::radsim::detail {
    struct RuntimeConfig {
        bool tbb_enabled = true;
        int32_t max_parallelism = 1;
        int32_t tasks_per_thread = 4;
    };

    constexpr int32_t kTasksPerThread = 4;

    inline RuntimeConfig g_runtime_config;

    inline bool g_runtime_config_explicit = false;

    inline void apply_runtime_config(bool use_tbb, int32_t requested_threads) {
        g_runtime_config.tbb_enabled = use_tbb;
        g_runtime_config.tasks_per_thread = kTasksPerThread;
        if (!use_tbb) {
            g_runtime_config.max_parallelism = 1;
            pool::Pool::instance().resize(1);
            return;
        }

        if (requested_threads > 0) {
            g_runtime_config.max_parallelism = requested_threads;
        } else {
            g_runtime_config.max_parallelism = pool::default_concurrency();
        }

        if (g_runtime_config.max_parallelism < 1)
            g_runtime_config.max_parallelism = 1;
        pool::Pool::instance().resize(g_runtime_config.max_parallelism);
    }

    inline const RuntimeConfig &runtime_config() {
        static const bool initialized = [] {
            if (!g_runtime_config_explicit) {
                apply_runtime_config(true, 0);
            }
            return true;
        }();
        (void)initialized;
        return g_runtime_config;
    }

    constexpr int32_t kChunksPerStepThread = 8;

    inline std::atomic<int32_t> g_step_width{0};

    inline int32_t step_parallelism() {
        const int32_t ceiling = runtime_config().max_parallelism;
        const int32_t width = g_step_width.load(std::memory_order_relaxed);
        return (width <= 0 || width > ceiling) ? ceiling : width;
    }

    inline int32_t step_target_task_count() {
        const auto &cfg = runtime_config();
        if (!cfg.tbb_enabled)
            return 1;
        return std::max(1, step_parallelism() * cfg.tasks_per_thread);
    }

    inline bool world_parallel_enabled(const RadWorld &world) {
        (void)world;
        return runtime_config().tbb_enabled && step_parallelism() > 1;
    }

    inline int32_t get_task_threshold(int32_t size, int32_t min_grain) {
        const int32_t grain = min_grain;
        if (size <= 0)
            return grain;
        const int32_t threshold = size / step_target_task_count();
        return std::max(grain, threshold);
    }

    inline int32_t derive_step_width(const RadWorld &world) {
        size_t chunks = 0u;
        for (const auto &bucket : world.chunks.parity_bucket_ids)
            chunks += bucket.size();
        const auto per_thread = static_cast<size_t>(kChunksPerStepThread);
        const auto ceiling = static_cast<size_t>(std::max(1, runtime_config().max_parallelism));
        const auto width = static_cast<int32_t>(std::min(chunks / per_thread, ceiling));
        return width < 1 ? 1 : width;
    }

    class StepWidthScope {
      public:
        explicit StepWidthScope(const RadWorld &world) {
            const int32_t width = derive_step_width(world);
            g_step_width.store(width, std::memory_order_relaxed);

            pool::Pool::instance().set_active_target(width);
        }

        ~StepWidthScope() {
            g_step_width.store(0, std::memory_order_relaxed);
            pool::Pool::instance().set_active_target(0);
        }

        StepWidthScope(const StepWidthScope &) = delete;
        StepWidthScope &operator=(const StepWidthScope &) = delete;
    };

    template <typename T> inline void prefetch_read(const T *ptr) noexcept {
        if (ptr == nullptr)
            return;
        __builtin_prefetch(ptr, 0, 3);
    }

    template <bool Wide> inline void prefetch_chunk_metadata(const ChunkTable &chunks, int32_t chunk_id) noexcept {
        if (chunk_id < 0)
            return;
        const auto idx = static_cast<size_t>(chunk_id);
        if (idx >= chunks.ck_by_id.size())
            return;
        const size_t metadata_base = Wide ? idx * static_cast<size_t>(chunks.words_per_chunk) : idx;
        prefetch_read(&chunks.kinds_by_id[metadata_base]);
        prefetch_read(&chunks.active_mask_by_id[metadata_base]);
        prefetch_read(chunks.section_id_by_sy.data() + idx * static_cast<size_t>(chunks.sections_per_chunk));
    }

    template <typename Work>
    inline void parallel_split_range(const RadWorld &world, int32_t lo, int32_t hi, int32_t threshold,
                                     const Work &work) {
        if (!world_parallel_enabled(world) || hi - lo <= threshold) {
            work(lo, hi);
            return;
        }
        const int32_t mid = (lo + hi) >> 1;
        pool::parallel_invoke([&] { parallel_split_range(world, lo, mid, threshold, work); },
                              [&] { parallel_split_range(world, mid, hi, threshold, work); });
    }

    template <typename Body>
    inline void parallel_for_range(const RadWorld &world, int32_t total, int32_t min_grain, const Body &body) {
        if (total <= 0)
            return;

        const int32_t threads = std::max(1, step_parallelism());
        const int32_t chunk = std::max(min_grain, total / (threads * 8));
        if (!world_parallel_enabled(world) || total <= chunk) {
            body(0, total);
            return;
        }
        std::atomic<int32_t> cursor{0};
        auto worker = [&] {
            for (;;) {
                const int32_t lo = cursor.fetch_add(chunk, std::memory_order_relaxed);
                if (lo >= total)
                    return;
                body(lo, std::min(lo + chunk, total));
            }
        };
        const int32_t slices = (total + chunk - 1) / chunk;
        const int32_t want = std::min(step_parallelism(), slices);
        pool::Pool::instance().invoke_n(want, worker);
    }

    template <typename... Work> inline void maybe_parallel_invoke(const RadWorld &world, Work &&...work) {
        if (world_parallel_enabled(world)) {
            pool::parallel_invoke(std::forward<Work>(work)...);
        } else {
            (std::forward<Work>(work)(), ...);
        }
    }

    template <typename... Work>
    inline void maybe_parallel_invoke_over(const RadWorld &world, int32_t elements, Work &&...work) {
        if (elements > kMinTaskGrain) {
            maybe_parallel_invoke(world, std::forward<Work>(work)...);
        } else {
            (std::forward<Work>(work)(), ...);
        }
    }
}

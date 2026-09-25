// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.threading;

import com.hbm.NuclearTech;
import com.hbm.config.BombConfig;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceCollection;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public final class BombForkJoinPool {

    private static final Object2ReferenceOpenHashMap<
                    Identifier, ReferenceOpenHashSet<IJobCancellable>>
            JOBS_BY_DIM = new Object2ReferenceOpenHashMap<>();
    private static final Object LOCK = new Object();
    private static ForkJoinPool pool;
    private static int refs;

    private BombForkJoinPool() {}

    public static ForkJoinPool acquire() {
        synchronized (LOCK) {
            if (pool == null || pool.isShutdown()) {
                int workers = computeWorkers();
                pool =
                        new ForkJoinPool(
                                workers,
                                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                                BombForkJoinPool::logWorkerFailure,
                                false);
            }
            refs++;
            return pool;
        }
    }

    public static void release(ForkJoinPool acquiredPool) {
        ForkJoinPool toShutdown;
        synchronized (LOCK) {
            if (acquiredPool != pool) return;
            if (refs <= 0) throw new IllegalStateException("BombForkJoinPool reference underflow");
            refs--;
            toShutdown = maybeShutdownLocked();
        }
        if (toShutdown != null) {
            toShutdown.shutdown();
        }
    }

    public static void register(
            ForkJoinPool acquiredPool, Identifier dimensionId, IJobCancellable job) {
        if (job == null || dimensionId == null || acquiredPool == null) return;
        synchronized (LOCK) {
            if (acquiredPool != pool) return;
            ReferenceOpenHashSet<IJobCancellable> set = JOBS_BY_DIM.get(dimensionId);
            if (set == null) {
                set = new ReferenceOpenHashSet<>();
                JOBS_BY_DIM.put(dimensionId, set);
            }
            set.add(job);
        }
    }

    public static void unregister(
            ForkJoinPool acquiredPool, Identifier dimensionId, IJobCancellable job) {
        if (job == null || dimensionId == null || acquiredPool == null) return;
        ForkJoinPool toShutdown = null;
        synchronized (LOCK) {
            if (acquiredPool != pool) return;
            ReferenceOpenHashSet<IJobCancellable> set = JOBS_BY_DIM.get(dimensionId);
            if (set == null) return;
            set.remove(job);
            if (set.isEmpty()) {
                JOBS_BY_DIM.remove(dimensionId);
                toShutdown = maybeShutdownLocked();
            }
        }
        if (toShutdown != null) {
            toShutdown.shutdown();
        }
    }

    public static void onLevelUnload(ServerLevel level) {
        if (level == null) return;
        onLevelUnload(level.dimension().identifier());
    }

    public static void onLevelUnload(Identifier dimensionId) {
        List<IJobCancellable> jobs = null;
        synchronized (LOCK) {
            ReferenceOpenHashSet<IJobCancellable> set = JOBS_BY_DIM.remove(dimensionId);
            if (set != null && !set.isEmpty()) {
                jobs = new ArrayList<>(set.size());
                jobs.addAll(set);
                set.clear();
            }
        }
        if (jobs != null) {
            for (IJobCancellable job : jobs) {
                try {
                    job.cancelJob();
                } catch (Throwable t) {
                    NuclearTech.LOGGER.error(
                            "Failed to cancel bomb job on dimension unload {}", dimensionId, t);
                }
            }
        }
        ForkJoinPool toShutdown;
        synchronized (LOCK) {
            toShutdown = maybeShutdownLocked();
        }
        if (toShutdown != null) {
            toShutdown.shutdown();
        }
    }

    public static boolean awaitQuiet(long timeoutMs) {
        ForkJoinPool current;
        synchronized (LOCK) {
            current = pool;
        }
        return current == null || current.awaitQuiescence(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public static void onServerStopped() {
        List<IJobCancellable> jobs = null;
        ForkJoinPool toStop;
        synchronized (LOCK) {
            ReferenceCollection<ReferenceOpenHashSet<IJobCancellable>> values =
                    JOBS_BY_DIM.values();
            if (!values.isEmpty()) {
                int approx = 0;
                for (ReferenceOpenHashSet<IJobCancellable> set : values) approx += set.size();
                jobs = new ArrayList<>(Math.max(approx, 16));
                for (ReferenceOpenHashSet<IJobCancellable> set : values) {
                    jobs.addAll(set);
                    set.clear();
                }
                JOBS_BY_DIM.clear();
            }
            refs = 0;
            toStop = pool;
            pool = null;
        }
        if (jobs != null) {
            for (IJobCancellable job : jobs) {
                try {
                    job.cancelJob();
                } catch (Throwable t) {
                    NuclearTech.LOGGER.error("Failed to cancel bomb job on server stop", t);
                }
            }
        }
        if (toStop != null && !toStop.isShutdown()) {

            toStop.shutdown();
        }
    }

    private static ForkJoinPool maybeShutdownLocked() {
        if (pool == null) return null;
        if (refs != 0) return null;
        if (!JOBS_BY_DIM.isEmpty()) return null;
        ForkJoinPool p = pool;
        pool = null;
        return p;
    }

    private static int computeWorkers() {
        int processors = Runtime.getRuntime().availableProcessors();
        int workers =
                BombConfig.maxThreads <= 0
                        ? Math.max(1, processors + BombConfig.maxThreads)
                        : Math.min(BombConfig.maxThreads, processors);
        return Math.max(1, workers);
    }

    private static void logWorkerFailure(Thread thread, Throwable error) {
        NuclearTech.LOGGER.error("Bomb ForkJoinPool worker crashed in {}", thread.getName(), error);
    }

    public interface IJobCancellable {
        void cancelJob();
    }
}

/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.entity.effect;

import com.hbm.NuclearTech;
import com.hbm.api.environment.Environment;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.config.BombConfig;
import com.hbm.config.FalloutConfigJSON.FalloutEntry;
import com.hbm.config.FalloutConfigJSON;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.item.FallingBlockBatchEntity;
import com.hbm.entity.item.FallingBlockNTEntity;
import com.hbm.entity.item.FallingMultiblockEntity;
import com.hbm.entity.item.PendingFallers;
import com.hbm.entity.logic.ExplosionChunkloadingEntity;
import com.hbm.explosion.FalloutJournal;
import com.hbm.handler.threading.BombForkJoinPool;
import com.hbm.lib.maps.NonBlockingHashMapLong;
import com.hbm.util.BiomeUtil;
import com.hbm.util.BulkSectionUpdates;
import com.hbm.util.ChunkRequests;
import com.hbm.util.ChunkUtil;
import com.hbm.util.SectionGeneration;
import com.hbm.util.SectionSnapshot;
import com.hbm.world.biome.NtmBiomes;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.lib.internal.UnsafeHolder.U;
import static com.hbm.lib.internal.UnsafeHolder.fieldOffset;

public class FalloutRainEntity extends ExplosionChunkloadingEntity
        implements BombForkJoinPool.IJobCancellable {

    static final ThreadLocal<WorkerScratch> TL_WORKER = ThreadLocal.withInitial(WorkerScratch::new);
    private static final EntityDataAccessor<Integer> DATA_SCALE =
            SynchedEntityData.defineId(FalloutRainEntity.class, EntityDataSerializers.INT);
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int MAX_SOLID_DEPTH = 3;
    private static final int WORKER_BATCH = 32;
    private static final float HARDNESS_BOUND = 6.0F;
    private static final long EMPTY_LONG = Long.MIN_VALUE;
    private static final long WAIT_INNER = 1L;
    private static final long WAIT_OUTER = 2L;
    private static final long OFF_PENDING_CHUNKS =
            fieldOffset(FalloutRainEntity.class, "pendingChunks");
    private static final long OFF_FINISHED = fieldOffset(FalloutRainEntity.class, "finished");
    private static final long OFF_FINISH_QUEUED =
            fieldOffset(FalloutRainEntity.class, "finishQueued");
    private static final long OFF_INNER_CURSOR =
            fieldOffset(FalloutRainEntity.class, "innerCursor");
    private static final long OFF_OUTER_CURSOR =
            fieldOffset(FalloutRainEntity.class, "outerCursor");
    private static final long OFF_POOL_ACQUIRED =
            fieldOffset(FalloutRainEntity.class, "poolAcquired");
    private static final long OFF_ACTIVE_WORKERS =
            fieldOffset(FalloutRainEntity.class, "activeWorkers");
    private static final long OFF_JOB_REGISTERED =
            fieldOffset(FalloutRainEntity.class, "jobRegistered");
    private final LongArrayList chunksToProcess = new LongArrayList();
    private final LongArrayList outerChunksToProcess = new LongArrayList();
    private final LongOpenHashSet workSet = new LongOpenHashSet();
    private final LongOpenHashSet readinessLoads = new LongOpenHashSet();
    private final NonBlockingHashMapLong<Long> waitingRoom = new NonBlockingHashMapLong<>();
    private final LongOpenHashSet evaluatedFallCores = new LongOpenHashSet();
    private @Nullable ChunkRequests chunkRequests;
    private final LongOpenHashSet publishedChunks = new LongOpenHashSet();
    private final LongOpenHashSet relighting = new LongOpenHashSet();
    private boolean gathered = false;
    private final int fDelay = BombConfig.fDelay;
    private int tickDelay = fDelay;
    private @Nullable ForkJoinPool pool;
    private @Nullable BulkSectionUpdates sectionUpdates;
    private volatile @Nullable Throwable failure;
    private boolean generationHeld;
    private @Nullable Identifier jobDimension;
    private @Nullable NonBlockingHashMapLong<LevelChunk> mirror;
    private @Nullable ConcurrentLinkedQueue<Long> qInner;
    private @Nullable ConcurrentLinkedQueue<Long> qOuter;
    private @Nullable ConcurrentLinkedQueue<Runnable> mainTasks;

    @SuppressWarnings("unused")
    private volatile int pendingChunks;

    @SuppressWarnings("unused")
    private volatile int finished;

    @SuppressWarnings("unused")
    private volatile int finishQueued;

    @SuppressWarnings("unused")
    private volatile int poolAcquired;

    @SuppressWarnings("unused")
    private volatile int activeWorkers;

    @SuppressWarnings("unused")
    private volatile int jobRegistered;

    @SuppressWarnings("unused")
    private volatile int innerCursor;

    @SuppressWarnings("unused")
    private volatile int outerCursor;

    protected List<FalloutEntry> falloutTable = List.of();
    private int workerScale;
    private long journalOp;
    private int workerInnerSize;
    private int workerOuterSize;
    private int workerTarget;
    private @Nullable Block layer;

    public FalloutRainEntity(EntityType<? extends FalloutRainEntity> type, Level level) {
        super(type, level);
    }

    public static FalloutRainEntity statFac(Level level, int scale, double x, double y, double z) {
        FalloutRainEntity rain = new FalloutRainEntity(ModEntities.FALLOUT_RAIN.get(), level);
        rain.setScale(scale);
        rain.setPos(x, y, z);
        return rain;
    }

    public FalloutRainEntity falloutLayer(Block layer) {
        this.layer = layer;
        return this;
    }

    private Block layer() {
        return layer != null ? layer : ModBlocks.FALLOUT.get();
    }

    protected static boolean isFlammable(BlockState state) {
        return ((FireBlock) Blocks.FIRE).getBurnOdds(state) > 0;
    }

    protected static BlockState readSection(
            ServerLevel server, LevelChunkSection[] sections, int lx, int y, int lz) {
        int subIdx = server.getSectionIndex(y);
        if (subIdx < 0 || subIdx >= sections.length) return AIR;
        LevelChunkSection section = sections[subIdx];
        return section.hasOnlyAir() ? AIR : section.getBlockState(lx, y & 15, lz);
    }

    private static void readPairs(int[] data, LongArrayList dest) {
        for (int i = 0; i + 1 < data.length; i += 2) {
            dest.add(ChunkPos.pack(data[i], data[i + 1]));
        }
    }

    private static int[] writePairs(LongArrayList src) {
        int[] data = new int[src.size() * 2];
        int j = 0;
        for (LongIterator it = src.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            data[j++] = ChunkPos.getX(packed);
            data[j++] = ChunkPos.getZ(packed);
        }
        return data;
    }

    public int getScale() {
        int value = entityData.get(DATA_SCALE);
        return value <= 0 ? 1 : value;
    }

    public void setScale(int scale) {
        entityData.set(DATA_SCALE, Math.max(1, scale));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SCALE, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;

        if (!gathered) {
            if (chunksToProcess.isEmpty() && outerChunksToProcess.isEmpty()) gatherChunks();
            gathered = true;
            if (getClass() != FalloutRainEntity.class) {
                startWorkersIfNeeded(server);
                if (isRemoved()) return;
            }
        }

        if (getClass() == FalloutRainEntity.class) {
            if (journalOp == 0) {
                if (tickDelay > 0) {
                    tickDelay--;
                    return;
                }
                startWorkersIfNeeded(server);
                if (isRemoved()) return;
            }
            if (FalloutJournal.settled(server, journalOp)) finishOnMain();
            return;
        }

        if (tickDelay <= 0) {
            tickDelay = fDelay;
            runServerThreadBudget(server, BombConfig.mk5);
        }
        tickDelay--;
    }

    private void startWorkersIfNeeded(ServerLevel server) {
        if (finished != 0) return;

        int innerSize = chunksToProcess.size();
        int outerSize = outerChunksToProcess.size();
        int total = innerSize + outerSize;
        if (total == 0) {
            U.putIntRelease(this, OFF_FINISHED, 1);
            discard();
            return;
        }

        falloutTable = FalloutConfigJSON.entries(server.registryAccess());
        if (getClass() == FalloutRainEntity.class) {
            if (journalOp == 0) {
                journalOp =
                        FalloutJournal.record(
                                server,
                                getX(),
                                getZ(),
                                getScale(),
                                layer(),
                                ExplosionData.CRATER_BIOMES.get() && !Environment.orbital(server));
            }
            if (generationHeld) {
                generationHeld = false;
                SectionGeneration.release();
            }
            releasePoolIfHeld();
            return;
        }

        initWorkStructures(server);
        U.putIntRelease(this, OFF_INNER_CURSOR, 0);
        U.putIntRelease(this, OFF_OUTER_CURSOR, 0);
        U.putIntRelease(this, OFF_ACTIVE_WORKERS, 0);
        U.putIntRelease(this, OFF_PENDING_CHUNKS, total);
        U.putIntRelease(this, OFF_FINISH_QUEUED, 0);

        mirror = ChunkUtil.acquireMirrorMap(server);
        if (U.compareAndSetInt(this, OFF_POOL_ACQUIRED, 0, 1)) {
            pool = BombForkJoinPool.acquire();
        }
        ForkJoinPool p = pool;
        if (p == null || p.isShutdown()) {
            U.putIntRelease(this, OFF_FINISHED, 1);
            cleanup(server);
            discard();
            return;
        }

        holdGenerationTracking();
        sectionUpdates = new BulkSectionUpdates(server, p, this::fail);

        workerScale = getScale();
        workerInnerSize = innerSize;
        workerOuterSize = outerSize;
        workerTarget = Math.max(1, Math.min(Math.max(1, p.getParallelism()), total));
        registerJobIfNeeded(server);
        maybeScheduleWorkers();
    }

    private void initWorkStructures(ServerLevel server) {
        if (qInner != null) return;
        qInner = new ConcurrentLinkedQueue<>();
        qOuter = new ConcurrentLinkedQueue<>();
        chunkRequests = new ChunkRequests(server, this::chunkReady);
        mainTasks = new ConcurrentLinkedQueue<>();
    }

    public void holdGenerationTracking() {
        ServerLevel server = (ServerLevel) level();
        assert server.getServer().isSameThread();
        if (!generationHeld) {
            SectionGeneration.acquire();
            generationHeld = true;
        }
        if (U.compareAndSetInt(this, OFF_POOL_ACQUIRED, 0, 1)) pool = BombForkJoinPool.acquire();
        registerJobIfNeeded(server);
    }

    private void registerJobIfNeeded(ServerLevel server) {
        if (U.compareAndSetInt(this, OFF_JOB_REGISTERED, 0, 1)) {
            Identifier dim = server.dimension().identifier();
            jobDimension = dim;
            BombForkJoinPool.register(pool, dim, this);
        }
    }

    private void unregisterJobIfNeeded() {
        if (U.getAndSetInt(this, OFF_JOB_REGISTERED, 0) != 0) {
            Identifier dim = jobDimension;
            jobDimension = null;
            if (dim != null) BombForkJoinPool.unregister(pool, dim, this);
        }
    }

    private void releasePoolIfHeld() {
        unregisterJobIfNeeded();
        if (U.getAndSetInt(this, OFF_POOL_ACQUIRED, 0) != 0) {
            BombForkJoinPool.release(pool);
            pool = null;
        }
    }

    private void runServerThreadBudget(ServerLevel server, long timeBudgetMs) {
        ConcurrentLinkedQueue<Runnable> tasks = mainTasks;
        if (tasks == null || chunkRequests == null) return;

        long deadline = System.nanoTime() + timeBudgetMs * 1_000_000L;
        if (sectionUpdates != null) sectionUpdates.drain(deadline);
        while (System.nanoTime() < deadline) {
            Runnable task = tasks.poll();
            if (task == null) break;
            task.run();
        }
        loadMissingChunksUntil(server, deadline);
        if (sectionUpdates != null) sectionUpdates.drain(deadline);
        maybeFinish();
    }

    private void loadMissingChunksUntil(ServerLevel server, long deadlineNanos) {
        ChunkRequests requests = chunkRequests;
        if (requests != null) requests.pump(deadlineNanos, BombConfig.chunksInFlight());
    }

    private void chunkReady(long packed) {
        NonBlockingHashMapLong<LevelChunk> m = mirror;
        if (m != null) m.put(packed, ChunkUtil.liveChunkNow((ServerLevel) level(), packed));
        readinessLoads.remove(packed);
        Long wait = waitingRoom.remove(packed);
        if (wait != null) enqueueWork(packed, wait == WAIT_OUTER);
    }

    private void enqueueWork(long cpLong, boolean outerRing) {
        if (outerRing) {
            ConcurrentLinkedQueue<Long> q = qOuter;
            if (q != null) q.offer(cpLong);
        } else {
            ConcurrentLinkedQueue<Long> q = qInner;
            if (q != null) q.offer(cpLong);
        }
        maybeScheduleWorkers();
    }

    private boolean hasImmediateWork() {
        ConcurrentLinkedQueue<Long> ri = qInner;
        if (ri != null && !ri.isEmpty()) return true;
        ConcurrentLinkedQueue<Long> ro = qOuter;
        if (ro != null && !ro.isEmpty()) return true;
        if (innerCursor < workerInnerSize) return true;
        return outerCursor < workerOuterSize;
    }

    private void maybeScheduleWorkers() {
        if (finished != 0) return;
        ForkJoinPool p = pool;
        if (p == null || p.isShutdown()) return;

        int target = workerTarget <= 0 ? 1 : workerTarget;
        while (true) {
            if (!hasImmediateWork()) return;
            int cur = activeWorkers;
            if (cur >= target) return;
            if (U.compareAndSetInt(this, OFF_ACTIVE_WORKERS, cur, cur + 1)) {
                p.submit(this::workerDrain);
            }
        }
    }

    private void workerDrain() {
        try {
            int processed = 0;
            while (processed < WORKER_BATCH) {
                if (finished != 0) return;

                long cpLong = EMPTY_LONG;
                boolean outerRing = false;

                ConcurrentLinkedQueue<Long> ri = qInner;
                if (ri != null) {
                    Long v = ri.poll();
                    if (v != null) cpLong = v;
                }
                if (cpLong == EMPTY_LONG) {
                    int idx = U.getAndAddInt(this, OFF_INNER_CURSOR, 1);
                    if (idx < workerInnerSize) {
                        cpLong = chunksToProcess.getLong(idx);
                    } else {
                        outerRing = true;
                        ConcurrentLinkedQueue<Long> ro = qOuter;
                        if (ro != null) {
                            Long v = ro.poll();
                            if (v != null) cpLong = v;
                        }
                        if (cpLong == EMPTY_LONG) {
                            int outerIdx = U.getAndAddInt(this, OFF_OUTER_CURSOR, 1);
                            if (outerIdx < workerOuterSize) {
                                cpLong = outerChunksToProcess.getLong(outerIdx);
                            }
                        }
                    }
                }

                if (cpLong == EMPTY_LONG) return;
                processChunkOffThread(cpLong, workerScale, outerRing);
                processed++;
            }
        } catch (Throwable cause) {
            fail(cause);
        } finally {
            U.getAndAddInt(this, OFF_ACTIVE_WORKERS, -1);
            if (finished == 0) maybeScheduleWorkers();
        }
    }

    private void processChunkOffThread(long packed, int scale, boolean outerRing) {
        if (finished != 0 || mirror == null) return;
        if (ChunkUtil.getLoadedChunk(mirror, packed) == null) {
            Long waitFlag = outerRing ? WAIT_OUTER : WAIT_INNER;
            Long previous = waitingRoom.putIfAbsent(packed, waitFlag);
            ChunkRequests requests = chunkRequests;
            if (previous == null && requests != null) requests.request(packed);
            return;
        }
        sectionUpdates.submit(new FalloutWork(packed, scale, outerRing));
    }

    private final class FalloutWork extends BulkSectionUpdates.Work {
        private final long packed;
        private final int scale;
        private final boolean outerRing;
        private final long seed = TL_WORKER.get().random.nextLong();
        private final Long2ObjectOpenHashMap<BlockState> oldStates = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<BlockState> deferred = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<BlockState> falling = new Long2ObjectOpenHashMap<>();
        private final LongOpenHashSet surfaces = new LongOpenHashSet();
        private boolean needsBiome;

        FalloutWork(long packed, int scale, boolean outerRing) {
            super(packed);
            this.packed = packed;
            this.scale = scale;
            this.outerRing = outerRing;
        }

        @Override
        protected long observedSections() {
            return -1L;
        }

        @Override
        protected void calculate(SectionSnapshot snapshot) {
            ServerLevel server = (ServerLevel) level();
            int chunkX = ChunkPos.getX(packed);
            int chunkZ = ChunkPos.getZ(packed);
            needsBiome =
                    ExplosionData.CRATER_BIOMES.get()
                            && getCraterBiomeKey(chunkX, chunkZ, scale) != null
                            && !Environment.orbital(server);
            LevelChunkSection[] sections = snapshot.sections;
            int minY = server.getMinY();
            int maxY = server.getMaxY();
            WorkerScratch scratch = TL_WORKER.get();
            scratch.clear();
            scratch.random.setSeed(seed);
            int topY = minY;
            for (int i = sections.length - 1; i >= 0; i--) {
                if (!sections[i].hasOnlyAir()) {
                    topY = Math.min(maxY, minY + (i << 4) + 15);
                    break;
                }
            }
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            for (int lx = 0; lx < 16; lx++) {
                int x = minX + lx;
                for (int lz = 0; lz < 16; lz++) {
                    int z = minZ + lz;
                    double dist = Math.hypot(x - getX(), z - getZ());
                    if (outerRing && dist > scale) continue;
                    double percent = scale <= 0 ? 100.0D : dist * 100.0D / scale;
                    stompColumn(server, sections, scratch, lx, lz, x, z, percent, minY, topY);
                }
            }
            ChunkUtil.applyToSnapshot(snapshot, scratch.updates, oldStates, deferred);
            falling.putAll(scratch.spawnFalling);
            surfaces.addAll(scratch.fallingSurfaceCells);
        }

        @Override
        protected boolean readyToPublish(LevelChunk chunk) {
            return neighbourhoodReady((ServerLevel) level(), packed);
        }

        @Override
        protected void published(LevelChunk chunk) {
            ServerLevel server = (ServerLevel) level();
            CompletableFuture<?> lighting = null;
            if (!oldStates.isEmpty()
                    || !deferred.isEmpty()
                    || !falling.isEmpty()
                    || !surfaces.isEmpty()
                    || needsBiome) {
                lighting =
                        doNotifyOnMain(
                                server, packed, oldStates, deferred, falling, surfaces, needsBiome);
            }
            settle(server, packed, lighting);
            if (U.getAndAddInt(FalloutRainEntity.this, OFF_PENDING_CHUNKS, -1) == 1) maybeFinish();
        }

        @Override
        protected void missing() {
            Long waitFlag = outerRing ? WAIT_OUTER : WAIT_INNER;
            Long previous = waitingRoom.putIfAbsent(packed, waitFlag);
            ChunkRequests requests = chunkRequests;
            if (previous == null && requests != null) requests.request(packed);
        }

        @Override
        protected void clearAttempt() {
            oldStates.clear();
            deferred.clear();
            falling.clear();
            surfaces.clear();
            needsBiome = false;
        }

        @Override
        protected void discard() {
            clearAttempt();
        }
    }

    protected void stompColumn(
            ServerLevel server,
            LevelChunkSection[] sections,
            WorkerScratch scratch,
            int lx,
            int lz,
            int x,
            int z,
            double percent,
            int minY,
            int maxY) {
        FalloutTerrain.stompColumn(
                server,
                sections,
                null,
                scratch,
                layer(),
                falloutTable,
                lx,
                lz,
                x,
                z,
                percent,
                minY,
                maxY);
    }

    private @Nullable CompletableFuture<?> doNotifyOnMain(
            ServerLevel server,
            long packed,
            @Nullable Long2ObjectOpenHashMap<BlockState> oldStates,
            @Nullable Long2ObjectOpenHashMap<BlockState> deferred,
            @Nullable Long2ObjectOpenHashMap<BlockState> spawnFalling,
            @Nullable LongOpenHashSet fallingSurfaceCells,
            boolean needsBiome) {
        int chunkX = ChunkPos.getX(packed);
        int chunkZ = ChunkPos.getZ(packed);

        if (needsBiome) applyCraterBiome(server, chunkX, chunkZ, workerScale);

        LevelChunk chunk = server.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) chunk = server.getChunkSource().getChunk(chunkX, chunkZ, true);

        long sectionMask = 0;
        LongArrayList lightChanges = new LongArrayList();
        int minSectionY = server.getMinSectionY();

        if (oldStates != null && !oldStates.isEmpty()) {
            Block fallout = layer();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            ObjectIterator<Long2ObjectMap.Entry<BlockState>> iterator =
                    oldStates.long2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Long2ObjectMap.Entry<BlockState> entry = iterator.next();
                long lp = entry.getLongKey();
                pos.set(BlockPos.getX(lp), BlockPos.getY(lp), BlockPos.getZ(lp));
                BlockState oldState = entry.getValue();
                BlockState newState = server.getBlockState(pos);

                if (newState.is(fallout) && !newState.canSurvive(server, pos)) {
                    server.setBlock(pos, oldState, 3);
                    continue;
                }
                if (oldState == newState) continue;

                if (ChunkUtil.postCarveBlockUpdate(server, chunk, pos, oldState, newState))
                    lightChanges.add(lp);
                server.updatePOIOnBlockStateChange(pos, oldState, newState);
                server.updateNeighborsAt(pos, newState.getBlock());
                newState.updateNeighbourShapes(server, pos, Block.UPDATE_CLIENTS);
                sectionMask |= 1L << ((pos.getY() >> 4) - minSectionY);
            }
        }

        if (deferred != null && !deferred.isEmpty()) {
            ObjectIterator<Long2ObjectMap.Entry<BlockState>> iterator =
                    deferred.long2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Long2ObjectMap.Entry<BlockState> entry = iterator.next();
                long lp = entry.getLongKey();
                BlockPos p = new BlockPos(BlockPos.getX(lp), BlockPos.getY(lp), BlockPos.getZ(lp));
                ChunkUtil.replaceEmptied(server, p, entry.getValue(), Block.UPDATE_ALL);
                sectionMask |= 1L << ((BlockPos.getY(lp) >> 4) - minSectionY);
            }
        }

        if (spawnFalling != null && !spawnFalling.isEmpty()) {
            if (PendingFallers.canDefer(
                    server,
                    new BlockPos(
                            chunk.getPos().getMinBlockX(),
                            server.getMinY(),
                            chunk.getPos().getMinBlockZ()))) {
                for (var iterator = spawnFalling.long2ObjectEntrySet().fastIterator();
                        iterator.hasNext(); ) {
                    var entry = iterator.next();
                    PendingFallers.defer(chunk, BlockPos.of(entry.getLongKey()), entry.getValue());
                }
            } else {
                spawnFallers(server, chunk, spawnFalling);
            }
            for (long lp : spawnFalling.keySet())
                sectionMask |= 1L << ((BlockPos.getY(lp) >> 4) - minSectionY);
        }

        if (fallingSurfaceCells != null && !fallingSurfaceCells.isEmpty()) {
            LongIterator surfaces = fallingSurfaceCells.iterator();
            while (surfaces.hasNext()) {
                long lp = surfaces.nextLong();
                long corePacked;
                BlockPos cellPos = BlockPos.of(lp);
                if (MultiblockSurface.foldedCore(server.getBlockState(cellPos)) != null) {
                    corePacked = lp;
                } else {
                    corePacked =
                            MultiblockSurface.indexedCorePacked(
                                    server,
                                    BlockPos.getX(lp),
                                    BlockPos.getY(lp),
                                    BlockPos.getZ(lp));
                }
                if (!MultiblockSurface.hasCore(corePacked)) continue;
                if (!evaluatedFallCores.add(corePacked)) continue;
                FallingMultiblockEntity.tryFall(server, BlockPos.of(corePacked));
            }
        }

        if (sectionMask == 0) return null;
        chunk.markUnsaved();
        CompletableFuture<?> lighting =
                ChunkUtil.updateLight(server, chunk, lightChanges, sectionMask);
        ChunkUtil.broadcastWholesaleResend(server, packed, lighting);
        return lighting;
    }

    public static void spawnFallers(
            ServerLevel server, LevelChunk chunk, Long2ObjectMap<BlockState> fallers) {
        FallingBlockBatchEntity batch = null;
        for (var entry : Long2ObjectMaps.fastIterable(fallers)) {
            BlockPos p = BlockPos.of(entry.getLongKey());
            BlockState captured = entry.getValue();
            if (FallingBlockBatchEntity.eligible(captured)) {
                if (batch == null) batch = FallingBlockBatchEntity.create(server, chunk);
                batch.add(p, captured);
            } else {
                FallingBlockNTEntity falling = FallingBlockNTEntity.fall(server, p, captured);
                falling.dropItem = false;
            }
        }
        if (batch != null && !server.addFreshEntity(batch)) {
            throw new IllegalStateException(
                    "Could not spawn fallout faller batch at " + chunk.getPos());
        }
    }

    private void settle(ServerLevel server, long packed, @Nullable CompletableFuture<?> lighting) {
        publishedChunks.add(packed);
        ConcurrentLinkedQueue<Runnable> tasks = mainTasks;
        if (lighting == null || lighting.isDone() || tasks == null) {
            releaseSettled(server, packed);
            return;
        }
        relighting.add(packed);
        lighting.whenComplete(
                (ignored, error) ->
                        tasks.offer(
                                () -> {
                                    relighting.remove(packed);
                                    releaseSettled(server, packed);
                                }));
    }

    private void releaseSettled(ServerLevel server, long packed) {
        int cx = ChunkPos.getX(packed);
        int cz = ChunkPos.getZ(packed);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long held = ChunkPos.pack(cx + dx, cz + dz);
                if (chunkRequests.holds(held) && settled(held)) chunkRequests.release(held);
            }
        }
    }

    private boolean settled(long packed) {
        int cx = ChunkPos.getX(packed);
        int cz = ChunkPos.getZ(packed);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long near = ChunkPos.pack(cx + dx, cz + dz);
                if (workSet.contains(near)
                        && (!publishedChunks.contains(near) || relighting.contains(near))) {
                    return false;
                }
            }
        }
        return true;
    }

    protected @Nullable ResourceKey<Biome> getCraterBiomeKey(int chunkX, int chunkZ, int scale) {
        double cx = (chunkX << 4) + 8;
        double cz = (chunkZ << 4) + 8;
        double dist = Math.hypot(cx - getX(), cz - getZ());
        double percent = scale <= 0 ? 100.0D : dist * 100.0D / scale;
        if (scale >= 150 && percent < 15) return NtmBiomes.CRATER_INNER;
        if (scale >= 100 && percent < 55) return NtmBiomes.CRATER;
        if (scale >= 25) return NtmBiomes.CRATER_OUTER;
        return null;
    }

    protected void applyCraterBiome(ServerLevel server, int chunkX, int chunkZ, int scale) {
        var reg = server.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<Biome> inner = reg.getOrThrow(NtmBiomes.CRATER_INNER);
        Holder<Biome> crater = reg.getOrThrow(NtmBiomes.CRATER);
        Holder<Biome> outer = reg.getOrThrow(NtmBiomes.CRATER_OUTER);
        BiomeUtil.paintCraterBiome(
                server,
                new ChunkPos(chunkX, chunkZ),
                getX(),
                getZ(),
                scale,
                inner,
                crater,
                outer,
                NtmBiomes.CRATER_INNER,
                NtmBiomes.CRATER,
                NtmBiomes.CRATER_OUTER);
    }

    private void maybeFinish() {
        if (finished != 0) return;
        if (pendingChunks != 0) return;
        if (sectionUpdates != null && sectionUpdates.hasPending()) return;
        if (!waitingRoom.isEmpty()) return;
        if (!U.compareAndSetInt(this, OFF_FINISH_QUEUED, 0, 1)) return;

        ConcurrentLinkedQueue<Runnable> tasks = mainTasks;
        if (tasks != null) tasks.offer(this::finishOnMain);
    }

    private void finishOnMain() {
        U.putIntRelease(this, OFF_FINISH_QUEUED, 0);

        if (finished != 0) return;
        if (journalOp != 0 && !FalloutJournal.settled((ServerLevel) level(), journalOp)) return;
        if (pendingChunks != 0) return;
        if (sectionUpdates != null && sectionUpdates.hasPending()) return;
        if (!waitingRoom.isEmpty()) return;

        U.putIntRelease(this, OFF_FINISHED, 1);
        if (!isRemoved()) discard();
    }

    private void fail(Throwable cause) {
        synchronized (this) {
            if (failure != null) return;
            failure = cause;
        }
        ServerLevel server = (ServerLevel) level();
        server.getServer()
                .execute(
                        () -> {
                            NuclearTech.LOGGER.error(
                                    "Fallout terrain work failed in {} at {}",
                                    server.dimension().identifier(),
                                    blockPosition(),
                                    cause);
                            cancelJob();
                        });
    }

    @Override
    public void cancelJob() {
        if (finished != 0) return;
        U.putIntRelease(this, OFF_FINISHED, 1);
        if (level() instanceof ServerLevel server) {
            if (server.getServer().isSameThread()) discard();
            else
                server.getServer()
                        .execute(
                                () -> {
                                    if (!isRemoved()) discard();
                                });
        } else if (!isRemoved()) {
            discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        cleanup(level() instanceof ServerLevel server ? server : null);
        super.remove(reason);
    }

    private void cleanup(@Nullable ServerLevel server) {
        if (sectionUpdates != null) sectionUpdates.close();
        if (generationHeld) {
            generationHeld = false;
            SectionGeneration.release();
        }
        U.putIntRelease(this, OFF_FINISHED, 1);
        releasePoolIfHeld();

        if (mirror != null && server != null) {
            ChunkUtil.releaseMirrorMap(server);
        }
        mirror = null;
        if (chunkRequests != null) {
            chunkRequests.close();
            chunkRequests = null;
        }

        if (qInner != null) {
            qInner.clear();
            qInner = null;
        }
        if (qOuter != null) {
            qOuter.clear();
            qOuter = null;
        }
        if (mainTasks != null) {
            mainTasks.clear();
            mainTasks = null;
        }
        workSet.clear();
        publishedChunks.clear();
        relighting.clear();

        waitingRoom.clear();
        readinessLoads.clear();
    }

    private void gatherChunks() {
        int radius = getScale();
        double px = getX();
        double pz = getZ();
        chunksToProcess.clear();
        outerChunksToProcess.clear();
        workSet.clear();
        int minChunkX = SectionPos.blockToSectionCoord(px - radius);
        int maxChunkX = SectionPos.blockToSectionCoord(px + radius);
        int minChunkZ = SectionPos.blockToSectionCoord(pz - radius);
        int maxChunkZ = SectionPos.blockToSectionCoord(pz + radius);
        for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                int ring = FalloutTerrain.ring(cx, cz, px, pz, radius);
                if (ring == FalloutTerrain.NONE) continue;
                long packed = ChunkPos.pack(cx, cz);
                workSet.add(packed);
                if (ring == FalloutTerrain.OUTER) outerChunksToProcess.add(packed);
                else chunksToProcess.add(packed);
            }
        }
    }

    private boolean neighbourhoodReady(ServerLevel server, long packed) {
        int cx = ChunkPos.getX(packed);
        int cz = ChunkPos.getZ(packed);
        boolean ready = true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long neighbour = ChunkPos.pack(cx + dx, cz + dz);
                if (!workSet.contains(neighbour)) continue;
                if (ChunkUtil.liveChunkNow(server, neighbour) == null) {
                    ready = false;
                    if (!chunkRequests.isPending(neighbour)
                            && !waitingRoom.containsKey(neighbour)
                            && readinessLoads.add(neighbour)) chunkRequests.request(neighbour);
                }
            }
        }
        return ready;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setScale(input.getIntOr("scale", 1));
        chunksToProcess.clear();
        outerChunksToProcess.clear();
        readPairs(input.getIntArray("chunks").orElse(new int[0]), chunksToProcess);
        readPairs(input.getIntArray("outerChunks").orElse(new int[0]), outerChunksToProcess);
        journalOp = input.getLongOr("journal_op", 0L);
        layer = input.read("fallout_layer", BuiltInRegistries.BLOCK.byNameCodec()).orElse(null);
        gathered = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("scale", getScale());
        if (journalOp != 0) output.putLong("journal_op", journalOp);
        output.putIntArray("chunks", writePairs(chunksToProcess));
        output.putIntArray("outerChunks", writePairs(outerChunksToProcess));
        if (layer != null)
            output.store("fallout_layer", BuiltInRegistries.BLOCK.byNameCodec(), layer);
    }

    public static final class WorkerScratch {
        public final Long2ObjectOpenHashMap<BlockState> updates =
                new Long2ObjectOpenHashMap<>(1024);
        public final Long2ObjectOpenHashMap<BlockState> spawnFalling =
                new Long2ObjectOpenHashMap<>(512);
        public final LongOpenHashSet fallingSurfaceCells = new LongOpenHashSet(64);
        public final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        public final RandomSource random = RandomSource.create();

        public void clear() {
            updates.clear();
            spawnFalling.clear();
            fallingSurfaceCells.clear();
        }
    }
}

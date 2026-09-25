// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.NuclearTech;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockVolcano;
import com.hbm.blocks.generic.BlockFallout;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.config.BombConfig;
import com.hbm.config.FalloutConfigJSON.FalloutEntry;
import com.hbm.config.FalloutConfigJSON;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityFallingBlockNT;
import com.hbm.entity.item.EntityFallingMultiblock;
import com.hbm.entity.logic.EntityExplosionChunkloading;
import com.hbm.handler.threading.BombForkJoinPool;
import com.hbm.interfaces.ServerThread;
import com.hbm.lib.maps.NonBlockingHashMapLong;
import com.hbm.lib.queues.MpscUnboundedXaddArrayLongQueue;
import com.hbm.util.BiomeUtil;
import com.hbm.util.BulkSectionUpdates;
import com.hbm.util.ChunkUtil;
import com.hbm.util.SectionGeneration;
import com.hbm.util.SectionSnapshot;
import com.hbm.world.biome.NtmBiomes;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.lib.internal.UnsafeHolder.U;
import static com.hbm.lib.internal.UnsafeHolder.fieldOffset;

public class EntityFalloutRain extends EntityExplosionChunkloading
        implements BombForkJoinPool.IJobCancellable {

    static final ThreadLocal<WorkerScratch> TL_WORKER = ThreadLocal.withInitial(WorkerScratch::new);
    private static final EntityDataAccessor<Integer> DATA_SCALE =
            SynchedEntityData.defineId(EntityFalloutRain.class, EntityDataSerializers.INT);
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int MAX_SOLID_DEPTH = 3;
    private static final int MIN_ANGLE_STEPS = 18;
    private static final int SPOKE_STEP_BLOCKS = 8;
    private static final int WORKER_BATCH = 32;
    private static final float HARDNESS_BOUND = 6.0F;
    private static final long EMPTY_LONG = MpscUnboundedXaddArrayLongQueue.EMPTY;
    private static final long WAIT_INNER = 1L;
    private static final long WAIT_OUTER = 2L;
    private static final long OFF_PENDING_CHUNKS =
            fieldOffset(EntityFalloutRain.class, "pendingChunks");
    private static final long OFF_FINISHED = fieldOffset(EntityFalloutRain.class, "finished");
    private static final long OFF_FINISH_QUEUED =
            fieldOffset(EntityFalloutRain.class, "finishQueued");
    private static final long OFF_INNER_CURSOR =
            fieldOffset(EntityFalloutRain.class, "innerCursor");
    private static final long OFF_OUTER_CURSOR =
            fieldOffset(EntityFalloutRain.class, "outerCursor");
    private static final long OFF_POOL_ACQUIRED =
            fieldOffset(EntityFalloutRain.class, "poolAcquired");
    private static final long OFF_ACTIVE_WORKERS =
            fieldOffset(EntityFalloutRain.class, "activeWorkers");
    private static final long OFF_JOB_REGISTERED =
            fieldOffset(EntityFalloutRain.class, "jobRegistered");
    private final LongArrayList chunksToProcess = new LongArrayList();
    private final LongArrayList outerChunksToProcess = new LongArrayList();

    private final LongOpenHashSet workSet = new LongOpenHashSet();
    private final LongOpenHashSet readinessLoads = new LongOpenHashSet();
    private final NonBlockingHashMapLong<Long> waitingRoom = new NonBlockingHashMapLong<>();

    private final LongOpenHashSet evaluatedFallCores = new LongOpenHashSet();

    private final Long2ObjectOpenHashMap<CompletableFuture<ChunkResult<LevelChunk>>>
            chunksInFlight = new Long2ObjectOpenHashMap<>();
    private final LongArrayList ticketsPendingFlush = new LongArrayList();
    private final LongOpenHashSet ticketedChunks = new LongOpenHashSet();
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
    private @Nullable MpscUnboundedXaddArrayLongQueue chunkLoadQueue;
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

    private List<FalloutEntry> falloutTable = List.of();
    private int workerScale;
    private int workerInnerSize;
    private int workerOuterSize;
    private int workerTarget;

    public EntityFalloutRain(EntityType<? extends EntityFalloutRain> type, Level level) {
        super(type, level);
    }

    public static EntityFalloutRain statFac(Level level, int scale, double x, double y, double z) {
        EntityFalloutRain rain = new EntityFalloutRain(ModEntities.FALLOUT_RAIN.get(), level);
        rain.setScale(scale);
        rain.setPos(x, y, z);
        return rain;
    }

    private static boolean isFlammable(BlockState state) {
        return ((FireBlock) Blocks.FIRE).getBurnOdds(state) > 0;
    }

    private static BlockState readSection(
            ServerLevel server, LevelChunkSection[] sections, int lx, int y, int lz) {
        int subIdx = server.getSectionIndex(y);
        if (subIdx < 0 || subIdx >= sections.length) return AIR;
        LevelChunkSection section = sections[subIdx];
        return section.hasOnlyAir() ? AIR : section.getBlockState(lx, y & 15, lz);
    }

    private static void addScanlineOrdered(LongOpenHashSet src, LongArrayList dst) {
        long[] keyed = new long[src.size()];
        int i = 0;
        for (LongIterator it = src.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            keyed[i++] =
                    (((long) (ChunkPos.getZ(packed) ^ Integer.MIN_VALUE)) << 32)
                            | ((ChunkPos.getX(packed) ^ Integer.MIN_VALUE) & 0xFFFFFFFFL);
        }
        Arrays.sort(keyed);
        for (long key : keyed) {
            dst.add(
                    ChunkPos.pack(
                            (int) key ^ Integer.MIN_VALUE, (int) (key >>> 32) ^ Integer.MIN_VALUE));
        }
    }

    private static int ceilPow2(int x) {
        if (x <= 1) return 1;
        int hb = Integer.highestOneBit(x - 1);
        int r = hb << 1;
        return r > 0 ? r : (1 << 30);
    }

    private static int chooseChunkSizeForSegments(
            int peakDepth, int targetSegments, int minPow2, int maxPow2) {
        if (peakDepth <= 0) return minPow2;
        int want = (peakDepth + targetSegments - 1) / targetSegments;
        int cs = ceilPow2(want);
        if (cs < minPow2) cs = minPow2;
        if (cs > maxPow2) cs = maxPow2;
        return cs;
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
            startWorkersIfNeeded(server);
            if (isRemoved()) return;
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

        initWorkStructures(total);
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

    private void initWorkStructures(int total) {
        if (qInner != null) return;
        qInner = new ConcurrentLinkedQueue<>();
        qOuter = new ConcurrentLinkedQueue<>();
        int csLoad = chooseChunkSizeForSegments(Math.max(total, 1024), 32, 1024, 8192);
        chunkLoadQueue = new MpscUnboundedXaddArrayLongQueue(csLoad, 4);
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
        MpscUnboundedXaddArrayLongQueue loadQueue = chunkLoadQueue;
        if (tasks == null || loadQueue == null) return;

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

    @ServerThread
    private void loadMissingChunksUntil(ServerLevel server, long deadlineNanos) {
        MpscUnboundedXaddArrayLongQueue loadQueue = chunkLoadQueue;
        if (loadQueue == null) return;

        while (System.nanoTime() < deadlineNanos
                && chunksInFlight.size() + ticketsPendingFlush.size()
                        < BombConfig.chunksInFlight()) {
            long packed = loadQueue.relaxedPoll();
            if (packed == EMPTY_LONG) break;
            if (chunksInFlight.containsKey(packed) || ticketsPendingFlush.contains(packed))
                continue;
            if (ticketedChunks.add(packed)) ChunkUtil.addBlastTicket(server, packed);
            ticketsPendingFlush.add(packed);
        }
        if (!ticketsPendingFlush.isEmpty() && System.nanoTime() < deadlineNanos) {
            ChunkUtil.flushChunkTickets(server);
            for (int i = 0; i < ticketsPendingFlush.size(); i++) {
                long packed = ticketsPendingFlush.getLong(i);
                CompletableFuture<ChunkResult<LevelChunk>> future =
                        ChunkUtil.blastChunkFuture(server, packed);
                if (future != null) chunksInFlight.put(packed, future);
                else loadQueue.offer(packed);
            }
            ticketsPendingFlush.clear();
        }

        if (chunksInFlight.isEmpty()) return;
        var it = chunksInFlight.long2ObjectEntrySet().fastIterator();
        while (it.hasNext() && System.nanoTime() < deadlineNanos) {
            var entry = it.next();
            CompletableFuture<ChunkResult<LevelChunk>> future = entry.getValue();
            if (!future.isDone()) continue;
            long packed = entry.getLongKey();
            it.remove();
            LevelChunk chunk = future.join().orElse(null);

            if (chunk != null) chunk = ChunkUtil.liveChunkNow(server, packed);

            if (chunk == null) {
                loadQueue.offer(packed);
                continue;
            }

            NonBlockingHashMapLong<LevelChunk> m = mirror;
            if (chunk != null && m != null) m.put(packed, chunk);
            readinessLoads.remove(packed);

            Long wait = waitingRoom.remove(packed);
            if (wait != null) enqueueWork(packed, wait == WAIT_OUTER);

            if (System.nanoTime() >= deadlineNanos) return;
        }
    }

    @ServerThread
    private void releaseChunkTickets(@Nullable ServerLevel server) {
        if (server != null) {
            for (long ck : ticketedChunks) ChunkUtil.releaseChunkAsync(server, ck);
        }
        ticketedChunks.clear();
        chunksInFlight.clear();
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
            if (previous == null && chunkLoadQueue != null) chunkLoadQueue.offer(packed);
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
                            && getCraterBiomeKey(chunkX, chunkZ, scale) != null;
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
            if (U.getAndAddInt(EntityFalloutRain.this, OFF_PENDING_CHUNKS, -1) == 1) maybeFinish();
        }

        @Override
        protected void missing() {
            Long waitFlag = outerRing ? WAIT_OUTER : WAIT_INNER;
            Long previous = waitingRoom.putIfAbsent(packed, waitFlag);
            if (previous == null && chunkLoadQueue != null) chunkLoadQueue.offer(packed);
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

    private void stompColumn(
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
        int solidDepth = 0;
        BlockPos.MutableBlockPos pos = scratch.pos;
        Long2ObjectOpenHashMap<BlockState> updates = scratch.updates;
        Long2ObjectOpenHashMap<BlockState> spawnFalling = scratch.spawnFalling;
        LongOpenHashSet fallingSurfaceCells = scratch.fallingSurfaceCells;
        RandomSource random = scratch.random;
        Block fallout = ModBlocks.FALLOUT.get();

        for (int y = maxY; y >= minY; y--) {
            if (solidDepth >= MAX_SOLID_DEPTH) return;

            BlockState state = readSection(server, sections, lx, y, lz);
            if (state.isAir()) continue;
            if (state.is(fallout)) continue;

            if (state.is(ModBlocks.VOLCANO_CORE.get())) {
                updates.put(
                        BlockPos.asLong(x, y, z),
                        ModBlocks.VOLCANO_RAD_CORE
                                .get()
                                .defaultBlockState()
                                .setValue(BlockVolcano.MODE, state.getValue(BlockVolcano.MODE)));
                continue;
            }

            BlockState aboveState = null;
            int upY = y + 1;
            if (solidDepth == 0 && upY <= maxY) {
                aboveState = readSection(server, sections, lx, upY, lz);
                boolean replaceable =
                        aboveState.isAir() || (aboveState.canBeReplaced() && !aboveState.liquid());
                if (replaceable) {
                    double d = percent / 100.0D;
                    double chance = 0.1D - Math.pow(d - 0.7D, 2);
                    if (chance >= random.nextDouble()) {
                        BlockState target;
                        if (aboveState.is(fallout)) {
                            int existing = aboveState.getValue(BlockFallout.LAYERS);
                            target =
                                    existing < SnowLayerBlock.MAX_HEIGHT
                                            ? aboveState.setValue(BlockFallout.LAYERS, existing + 1)
                                            : null;
                        } else {
                            target = fallout.defaultBlockState();
                        }
                        if (target != null) updates.put(BlockPos.asLong(x, upY, z), target);
                    }
                }
            }

            if (percent < 65D && isFlammable(state) && upY <= maxY) {
                if (aboveState == null) aboveState = readSection(server, sections, lx, upY, lz);
                if (aboveState.isAir() && random.nextInt(5) == 0) {
                    updates.put(BlockPos.asLong(x, upY, z), Blocks.FIRE.defaultBlockState());
                }
            }

            boolean transformed = false;
            List<FalloutEntry> entries = falloutTable;
            for (int i = 0, n = entries.size(); i < n; i++) {
                FalloutEntry entry = entries.get(i);
                BlockState result = entry.eval(server, pos.set(x, y, z), state, percent, random);
                if (result != null) {
                    updates.put(BlockPos.asLong(x, y, z), result);
                    if (entry.restrictDepth()) solidDepth++;
                    transformed = true;
                    break;
                }
            }

            if (!transformed
                    && y > minY
                    && percent < 65D
                    && MultiblockSurface.isSurface(state)
                    && readSection(server, sections, lx, y - 1, lz).isAir()) {
                fallingSurfaceCells.add(BlockPos.asLong(x, y, z));
            }

            if (y > minY && percent < 65D && !MultiblockSurface.isSurface(state)) {
                float hardness = state.getDestroySpeed(server, pos.set(x, y, z));
                if (hardness >= 0F && hardness <= HARDNESS_BOUND) {
                    BlockState belowState = readSection(server, sections, lx, y - 1, lz);
                    if (belowState.isAir()) {
                        for (int i = 0; i <= solidDepth; i++) {
                            int yy = y + i;
                            if (yy > maxY) break;
                            BlockState colState = updates.get(BlockPos.asLong(x, yy, z));
                            if (colState == null)
                                colState = readSection(server, sections, lx, yy, lz);
                            if (colState.isAir()) continue;
                            float h = colState.getDestroySpeed(server, pos.set(x, yy, z));
                            if (h >= 0F && h <= HARDNESS_BOUND) {
                                spawnFalling.putIfAbsent(BlockPos.asLong(x, yy, z), colState);
                            }
                        }
                    }
                }
            }

            if (!transformed && state.canOcclude()) solidDepth++;
        }
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
            Block fallout = ModBlocks.FALLOUT.get();
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
            ObjectIterator<Long2ObjectMap.Entry<BlockState>> iterator =
                    spawnFalling.long2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Long2ObjectMap.Entry<BlockState> entry = iterator.next();
                long lp = entry.getLongKey();
                BlockState captured = entry.getValue();
                int py = BlockPos.getY(lp);
                BlockPos p = new BlockPos(BlockPos.getX(lp), py, BlockPos.getZ(lp));
                EntityFallingBlockNT falling = EntityFallingBlockNT.fall(server, p, captured);
                falling.dropItem = false;
                sectionMask |= 1L << ((py >> 4) - minSectionY);
            }
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
                EntityFallingMultiblock.tryFall(server, BlockPos.of(corePacked));
            }
        }

        if (sectionMask == 0) return null;
        chunk.markUnsaved();
        CompletableFuture<?> lighting =
                ChunkUtil.updateLight(server, chunk, lightChanges, sectionMask);
        ChunkUtil.broadcastWholesaleResend(server, packed, lighting);
        return lighting;
    }

    @ServerThread
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

    @ServerThread
    private void releaseSettled(ServerLevel server, long packed) {
        int cx = ChunkPos.getX(packed);
        int cz = ChunkPos.getZ(packed);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long held = ChunkPos.pack(cx + dx, cz + dz);
                if (ticketedChunks.contains(held) && settled(held)) {
                    ticketedChunks.remove(held);
                    ChunkUtil.releaseChunkAsync(server, held);
                }
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

    private @Nullable ResourceKey<Biome> getCraterBiomeKey(int chunkX, int chunkZ, int scale) {
        double cx = (chunkX << 4) + 8;
        double cz = (chunkZ << 4) + 8;
        double dist = Math.hypot(cx - getX(), cz - getZ());
        double percent = scale <= 0 ? 100.0D : dist * 100.0D / scale;

        if (scale >= 150 && percent < 15) return NtmBiomes.CRATER_INNER;
        if (scale >= 100 && percent < 55) return NtmBiomes.CRATER;
        if (scale >= 25) return NtmBiomes.CRATER_OUTER;
        return null;
    }

    private void applyCraterBiome(ServerLevel server, int chunkX, int chunkZ, int scale) {
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

        releaseChunkTickets(server);

        if (qInner != null) {
            qInner.clear();
            qInner = null;
        }
        if (qOuter != null) {
            qOuter.clear();
            qOuter = null;
        }
        if (chunkLoadQueue != null) {
            chunkLoadQueue.clear();
            chunkLoadQueue = null;
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
        int angleSteps = 20 * radius / 32;
        if (angleSteps < MIN_ANGLE_STEPS) angleSteps = MIN_ANGLE_STEPS;

        int steps = angleSteps + 1;
        LongOpenHashSet outer = new LongOpenHashSet(steps);
        int radialSteps = radius / SPOKE_STEP_BLOCKS + 1;
        int candidateUpper = radialSteps * steps;
        double rc = (radius + 16.0) / 16.0;
        int diskUpper = (int) Math.ceil(Math.PI * rc * rc + 2.0 * Math.PI * rc + 32.0);
        int innerExpected = Math.min(candidateUpper, diskUpper);
        LongOpenHashSet inner = new LongOpenHashSet(innerExpected);

        double px = getX();
        double pz = getZ();

        double[] cos = new double[steps];
        double[] sin = new double[steps];
        for (int step = 0; step <= angleSteps; step++) {
            double theta = step * (2.0 * Math.PI) / angleSteps;
            cos[step] = Math.cos(theta);
            sin[step] = Math.sin(theta);
        }

        for (int step = 0; step <= angleSteps; step++) {
            int cx = SectionPos.blockToSectionCoord(px + radius * cos[step]);
            int cz = SectionPos.blockToSectionCoord(pz - radius * sin[step]);
            outer.add(ChunkPos.pack(cx, cz));
        }

        for (int d = 0; d <= radius; d += SPOKE_STEP_BLOCKS) {
            for (int step = 0; step <= angleSteps; step++) {
                int cx = SectionPos.blockToSectionCoord(px + d * cos[step]);
                int cz = SectionPos.blockToSectionCoord(pz - d * sin[step]);
                long packed = ChunkPos.pack(cx, cz);
                if (!outer.contains(packed)) inner.add(packed);
            }
        }

        chunksToProcess.clear();
        outerChunksToProcess.clear();
        workSet.clear();
        workSet.addAll(inner);
        workSet.addAll(outer);
        addScanlineOrdered(inner, chunksToProcess);
        addScanlineOrdered(outer, outerChunksToProcess);
    }

    @ServerThread
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

                    if (!chunksInFlight.containsKey(neighbour)
                            && !waitingRoom.containsKey(neighbour)
                            && readinessLoads.add(neighbour)) chunkLoadQueue.offer(neighbour);
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
        gathered = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("scale", getScale());
        output.putIntArray("chunks", writePairs(chunksToProcess));
        output.putIntArray("outerChunks", writePairs(outerChunksToProcess));
    }

    static final class WorkerScratch {
        final Long2ObjectOpenHashMap<BlockState> updates = new Long2ObjectOpenHashMap<>(1024);
        final Long2ObjectOpenHashMap<BlockState> spawnFalling = new Long2ObjectOpenHashMap<>(512);
        final LongOpenHashSet fallingSurfaceCells = new LongOpenHashSet(64);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final RandomSource random = RandomSource.create();

        void clear() {
            updates.clear();
            spawnFalling.clear();
            fallingSurfaceCells.clear();
        }
    }
}

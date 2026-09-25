// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.NuclearTech;
import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.interfaces.ServerThread;
import com.hbm.lib.Library;
import com.hbm.lib.internal.natives.RadsimWorld;
import com.hbm.lib.queues.MpscUnboundedXaddArrayLongQueue;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.particle.HbmEffectNT;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.DecodeException;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BitStorage;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class RadiationSystemNT {

    static final int MAX_POCKETS = 2048;
    static final int DIRECT_OVERLAP_MAX_OLD_POCKETS = 16, DIRECT_OVERLAP_MAX_CELLS = 2048;
    static final short NO_POCKET = -1;
    static final int KIND_NONE = 0, KIND_UNI = 1, KIND_SINGLE = 2, KIND_MULTI = 3;
    static final int MAX_SECTIONS_PER_CHUNK = DimensionType.Y_SIZE / SectionPos.SECTION_SIZE;
    static final long CHUNK_DIRTY_MASK = 1L << 63;

    static final int[] FACE_DX = {0, 0, 0, 0, -1, 1},
            FACE_DY = {-1, 1, 0, 0, 0, 0},
            FACE_DZ = {0, 0, -1, 1, 0, 0};
    static final int[] FACE_PLANE = new int[6 * 256];

    static final Map<ServerLevel, WorldRadiationData> worldMap = new HashMap<>(4);
    private static final Map<ServerLevel, Double> disabledAmbient = new HashMap<>(4);
    static final int[] BOUNDARY_MASKS = {0, 0, 0xF00, 0xF00, 0xFF0, 0xFF0},
            LINEAR_OFFSETS = {-256, 256, -16, 16, -1, 1};
    static final int PROFILE_WINDOW = 200;
    static final int FOG_DRAIN_LIMIT = 8;
    static final byte MAGIC_0 = (byte) 'N', MAGIC_1 = (byte) 'T', MAGIC_2 = (byte) 'X', FMT = 8;
    static final int FMT8_COMPACT_BYTES = 5 + 32 * (4 + 1 + 2 + 2 + 512 + MAX_POCKETS * 10);
    static final int FMT8_MAX_BYTES =
            5 + MAX_SECTIONS_PER_CHUNK * (4 + 1 + 2 + 2 + 512 + MAX_POCKETS * 10);
    static final int SIDECAR_CARRY_HEADER_BYTES = Long.BYTES;
    static final ReferenceOpenHashSet<BlockState> RAD_RESISTANT_STATES =
            new ReferenceOpenHashSet<>(64);
    static final Reference2ObjectOpenHashMap<BlockState, RadSource> RAD_SOURCE_STATES =
            new Reference2ObjectOpenHashMap<>(64);
    static final ForkJoinPool RAD_POOL = ForkJoinPool.commonPool();
    static final int TARGET_TASK_CNT = RAD_POOL.getParallelism() << 2;
    static final ChunkRadiationStorage SIDE_CAR = new RegionChunkRadiationStorage();
    static final ConcurrentHashMap<ResourceKey<Level>, Long> sidecarEpochs =
            new ConcurrentHashMap<>();
    static final ThreadLocal<int[]> TL_FF_QUEUE =
            ThreadLocal.withInitial(() -> new int[SectionPos.SECTION_BLOCK_COUNT]);
    static final ThreadLocal<PalScratch> TL_PAL_SCRATCH = ThreadLocal.withInitial(PalScratch::new);
    static final ThreadLocal<int[]> TL_VOL_COUNTS =
            ThreadLocal.withInitial(() -> new int[MAX_POCKETS]);
    static final ThreadLocal<double[]> TL_NEW_MASS =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS]);
    static final ThreadLocal<long[]> TL_SUM_XYZ =
            ThreadLocal.withInitial(() -> new long[MAX_POCKETS * 3]);
    static final ThreadLocal<double[]> TL_DENSITIES =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS]);
    static final ThreadLocal<double[]> TL_ADD =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS + 1]);
    static final ThreadLocal<double[]> TL_SET =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS + 1]);
    static final ThreadLocal<long[]> TL_BEST_SET_SEQ =
            ThreadLocal.withInitial(() -> new long[MAX_POCKETS + 1]);
    static final ThreadLocal<double[]> TL_SRC_WEIGHT =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS + 1]);
    static final ThreadLocal<double[]> TL_SRC_NUMERATOR =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS + 1]);
    static final ThreadLocal<int[]> TL_TEMP_ARRAY =
            ThreadLocal.withInitial(() -> new int[MAX_POCKETS]);
    static final ThreadLocal<int[]> TL_TOUCHED = ThreadLocal.withInitial(() -> new int[512]);
    static final ThreadLocal<Long2IntOpenHashMap> TL_EDGE_COUNTS =
            ThreadLocal.withInitial(
                    () -> {
                        Long2IntOpenHashMap map = new Long2IntOpenHashMap(512);
                        map.defaultReturnValue(0);
                        return map;
                    });
    static final ThreadLocal<ByteBuffer> TL_ENCODE_BUF =
            ThreadLocal.withInitial(() -> ByteBuffer.allocate(FMT8_COMPACT_BYTES - 4));
    static final ThreadLocal<double[]> TL_TEMP_DENSITIES =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS]);
    static final ThreadLocal<double[]> TL_DIFF_RECIPROCALS =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS]);
    static final double RAD_EPSILON = 1.0e-5D;
    static final double RAD_MAX = Double.MAX_VALUE / 2.0D;
    static final long DESTROY_PROB_U64 = Long.divideUnsigned(-1L, 100L);
    static final CompletableFuture<Void> COMPLETED = CompletableFuture.completedFuture(null);
    static volatile int resistantStateVersion;
    static long ticks;
    static @NonNull CompletableFuture<Void> radiationFuture = COMPLETED;
    static boolean serverStopping;

    static int tickDelay = 1;
    static double dT = tickDelay / (double) SharedConstants.TICKS_PER_SECOND;

    static {
        int[] rowShifts = {4, 4, 8, 8, 8, 8},
                colShifts = {0, 0, 0, 0, 4, 4},
                bases = {0, 15 << 8, 0, 15 << 4, 0, 15};
        for (int face = 0; face < 6; face++) {
            int base = face << 8;
            int rowShift = rowShifts[face];
            int colShift = colShifts[face];
            int fixedBits = bases[face];
            int t = 0;
            for (int r = 0; r < 16; r++) {
                int rBase = r << rowShift;
                for (int c = 0; c < 16; c++) {
                    FACE_PLANE[base + (t++)] = rBase | (c << colShift) | fixedBits;
                }
            }
        }
    }

    private RadiationSystemNT() {}

    static int getTaskThreshold(int size, int minGrain) {
        int th = size / TARGET_TASK_CNT;
        return Math.max(minGrain, th);
    }

    public static void onLoadComplete() {
        tickDelay = RadiationConfig.radTickRate;
        if (tickDelay <= 0) throw new IllegalStateException("Radiation tick rate must be positive");
        dT = tickDelay / (double) SharedConstants.TICKS_PER_SECOND;
        double diffusivity = RadiationData.RAD_DIFFUSIVITY.get();
        if (diffusivity <= 0.0D || !Double.isFinite(diffusivity))
            throw new IllegalStateException("Radiation diffusivity must be positive and finite");
        double hl = RadiationData.RAD_HALF_LIFE_SECONDS.get();
        if (hl <= 0.0D || !Double.isFinite(hl))
            throw new IllegalStateException("Radiation HalfLife must be positive and finite");
    }

    public static void applyBackendParams(RadsimWorld world, RadiationSettings.Resolved settings) {
        world.setParams(
                settings.diffusionDt(),
                settings.uuE(),
                settings.retentionDt(),
                settings.fogProbU64(),
                DESTROY_PROB_U64,
                settings.fogRad(),
                RAD_EPSILON,
                RAD_MAX);
    }

    static long probU64(double p) {
        if (!(p > 0.0D) || !Double.isFinite(p)) return 0L;
        if (p >= 1.0D) return -1L;
        double v = p * 4294967296.0D;
        long hi = (long) v;
        if (hi >= 0x1_0000_0000L) return -1L;
        double frac = v - (double) hi;
        long lo = (long) (frac * 4294967296.0D);
        if (lo >= 0x1_0000_0000L) lo = 0xFFFF_FFFFL;
        return (hi << 32) | (lo & 0xFFFF_FFFFL);
    }

    public static void init() {
        Services.SERVER.onServerTickPost(RadiationSystemNT::tickSim);
        Services.SERVER.onChunkLoad(RadiationSystemNT::onChunkLoad);
        Services.SERVER.onChunkUnload(RadiationSystemNT::onChunkUnload);
        Services.SERVER.onServerStopping(RadiationSystemNT::onServerStopping);
    }

    static void tickSim(MinecraftServer server) {
        serverStopping = false;
        if (!RadiationData.ENABLE_CHUNK_RADS.get()) return;
        ticks++;
        if ((ticks + 17) % tickDelay != 0) return;
        awaitSimulation();
        runWorldEffects();

        runRebuilds();
        RadVisServer.publish(server);
        radiationFuture = CompletableFuture.runAsync(RadiationSystemNT::runSweeps, RAD_POOL);
    }

    static void awaitSimulation() {
        if (radiationFuture == COMPLETED) return;
        try {
            radiationFuture.join();
        } catch (RuntimeException ex) {
            NuclearTech.LOGGER.error("Radiation async step failed", ex);
        } finally {
            radiationFuture = COMPLETED;
        }
    }

    @ServerThread
    static void runRebuilds() {
        for (WorldRadiationData data : worldMap.values()) {
            if (data.world.getServer() == null) continue;
            try {
                data.rebuildLoadedSections();
            } catch (Throwable t) {
                NuclearTech.LOGGER.error(
                        "Error in radiation rebuild in dimension {}",
                        data.world.dimension().identifier(),
                        t);
            }
        }
    }

    @ServerThread
    static void runWorldEffects() {
        boolean destroy = RadiationData.WORLD_RAD_EFFECTS.get();
        for (WorldRadiationData data : worldMap.values()) {
            if (data.world.getServer() == null) continue;
            try {
                if (destroy) handleWorldDestruction(data);
                runFog(data);
            } catch (Throwable t) {
                NuclearTech.LOGGER.error(
                        "Error in radiation world effects in dimension {}",
                        data.world.dimension().identifier(),
                        t);
            }
        }
    }

    @ServerThread
    static void runFog(WorldRadiationData data) {
        for (int i = 0; i < FOG_DRAIN_LIMIT; i++) {
            long pk = data.fogQueue.poll();
            if (pk == MpscUnboundedXaddArrayLongQueue.EMPTY) break;
            spawnFog(data, pk);
        }
        if (data.workEpoch % 200 == 13) data.fogQueue.clear(true);
    }

    @ServerThread
    static void spawnFog(WorldRadiationData data, long pk) {
        long ck = Library.sectionToChunkLong(pk);
        int ownerId = data.getId(ck);
        if (ownerId < 0 || ownerId >= data.nextId || data.cks[ownerId] != ck) return;
        LevelChunk chunk = data.mcChunks[ownerId];
        if (chunk == null) return;

        int yz = (int) (pk & 0xFFFFF);
        int slot = yz >>> 11;
        int targetPocketIndex = yz & 0x7FF;
        if (slot >= data.sectionsPerChunk) return;

        int kind = data.getKind(ownerId, slot);
        if (kind == KIND_NONE) return;
        LevelChunkSection[] sections = chunk.getSections();
        LevelChunkSection section = (slot < sections.length) ? sections[slot] : null;

        ServerLevel world = data.world;
        RandomSource rand = world.getRandom();
        int baseX = chunk.getPos().x() << 4;
        int baseZ = chunk.getPos().z() << 4;
        int baseY = (data.minSectionY + slot) << 4;
        int secIdx = (ownerId * data.sectionsPerChunk) + slot;
        WorldRadiationData.SectionRef sc = (kind == KIND_UNI) ? null : data.complexSecs[secIdx];
        if (kind != KIND_UNI && sc == null) return;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        for (int k = 0; k < 10; k++) {
            int lx = rand.nextInt(16), ly = rand.nextInt(16), lz = rand.nextInt(16);
            int x = baseX + lx, y = baseY + ly, z = baseZ + lz;
            if (sc != null && sc.getPocketIndex(BlockPos.asLong(x, y, z)) != targetPocketIndex)
                continue;
            BlockState state =
                    (section == null || section.hasOnlyAir())
                            ? Blocks.AIR.defaultBlockState()
                            : section.getBlockState(lx, ly, lz);
            if (!state.isAir()) continue;

            boolean nearGround = false;
            for (int d = 1; d <= 6; d++) {
                int yy = y - d;
                if (yy < world.getMinY()) break;
                if (!world.getBlockState(probe.set(x, yy, z)).isAir()) {
                    nearGround = true;
                    break;
                }
            }
            if (!nearGround) continue;

            double fx = x + 0.5D, fy = y + 0.5D, fz = z + 0.5D;
            Services.NETWORK.sendToAllAround(
                    new EffectNTPayload(HbmEffectNT.RadFog, fx, fy, fz),
                    new TargetPoint(world, fx, fy, fz, 100));
            return;
        }
    }

    @ServerThread
    static void handleWorldDestruction(WorldRadiationData data) {
        if (tickDelay == 1) {
            long pk = data.pocketToDestroy;
            data.pocketToDestroy = Long.MIN_VALUE;
            if (pk != Long.MIN_VALUE) destroyPocket(data, pk);
            return;
        }

        for (int i = 0; i < tickDelay; i++) {
            long pk = data.destructionQueue.poll();
            if (pk == MpscUnboundedXaddArrayLongQueue.EMPTY) break;
            destroyPocket(data, pk);
        }
        if (data.workEpoch % 200 == 13) data.destructionQueue.clear(true);
    }

    @ServerThread
    static void destroyPocket(WorldRadiationData data, long pk) {
        long ck = Library.sectionToChunkLong(pk);
        int ownerId = data.getId(ck);
        if (ownerId < 0 || ownerId >= data.nextId || data.cks[ownerId] != ck) return;
        LevelChunk chunk = data.mcChunks[ownerId];
        if (chunk == null) return;

        int yz = (int) (pk & 0xFFFFF);
        int slot = yz >>> 11;
        int targetPocketIndex = yz & 0x7FF;
        if (slot >= data.sectionsPerChunk) return;

        int kind = data.getKind(ownerId, slot);
        if (kind == KIND_NONE) return;

        LevelChunkSection[] sections = chunk.getSections();
        if (slot >= sections.length) return;
        LevelChunkSection section = sections[slot];
        if (section == null || section.hasOnlyAir()) return;

        ServerLevel world = data.world;
        RandomSource rand = world.getRandom();
        int baseX = chunk.getPos().x() << 4;
        int baseZ = chunk.getPos().z() << 4;
        int baseY = (data.minSectionY + slot) << 4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        if (kind == KIND_UNI) {
            if (targetPocketIndex != 0) return;
            for (int i = 0; i < SectionPos.SECTION_BLOCK_COUNT; i++) {
                if (rand.nextInt(3) != 0) continue;
                decayLocal(world, chunk, section, baseX, baseY, baseZ, i, pos);
            }
            return;
        }

        int secIdx = (ownerId * data.sectionsPerChunk) + slot;
        WorldRadiationData.SectionRef sc = data.complexSecs[secIdx];
        if (sc == null) return;
        for (int i = 0; i < SectionPos.SECTION_BLOCK_COUNT; i++) {
            if (rand.nextInt(3) != 0) continue;
            int actual = sc.paletteIndexOrNeg(i);
            if (actual < 0 || actual != targetPocketIndex) continue;
            decayLocal(world, chunk, section, baseX, baseY, baseZ, i, pos);
        }
    }

    @ServerThread
    static void decayLocal(
            ServerLevel world,
            LevelChunk chunk,
            LevelChunkSection section,
            int baseX,
            int baseY,
            int baseZ,
            int i,
            BlockPos.MutableBlockPos pos) {
        int lx = Library.getLocalX(i), ly = Library.getLocalY(i), lz = Library.getLocalZ(i);
        BlockState state = section.getBlockState(lx, ly, lz);
        if (state.isAir()) return;
        int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
        int myY = baseY + ly;
        if (myY < topY - 1 || myY > topY) return;
        pos.set(baseX + lx, myY, baseZ + lz);
        RadiationWorldHandler.decayBlock(world, pos, state);
    }

    @ServerThread
    static void writeSidecar(
            ServerLevel world, ChunkPos pos, byte @Nullable [] payload, boolean logFailure) {
        try {
            SIDE_CAR.write(world, pos, payload);
        } catch (RuntimeException ex) {
            if (logFailure) {
                NuclearTech.LOGGER.error(
                        "Radiation sidecar write failed for chunk {} in dimension {}",
                        pos,
                        world.dimension().identifier(),
                        ex);
            }
        }
    }

    static void saveDirtySidecar(boolean flush) {
        RuntimeException failure = null;
        for (WorldRadiationData data : worldMap.values()) {
            try {
                data.saveRadiationDirty(flush);
            } catch (RuntimeException ex) {
                if (!flush) {
                    NuclearTech.LOGGER.error(
                            "Radiation sidecar save failed for dimension {}",
                            data.world.dimension().identifier(),
                            ex);
                } else if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        if (flush) {
            try {
                SIDE_CAR.flush();
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
        }
        if (failure != null) throw failure;
    }

    public static void onServerSave(MinecraftServer server, boolean flush) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get()) return;
        awaitSimulation();
        saveDirtySidecar(flush);
    }

    static void onServerStopping(MinecraftServer server) {
        serverStopping = true;
        RuntimeException simulationFailure = null;
        try {
            radiationFuture.join();
        } catch (RuntimeException ex) {
            simulationFailure = ex;
        } finally {
            radiationFuture = COMPLETED;
        }
        try {
            saveDirtySidecar(true);
        } finally {
            for (WorldRadiationData data : worldMap.values()) data.logLifetimeProfiling();
            try {
                SIDE_CAR.close();
            } catch (RuntimeException ex) {
                NuclearTech.LOGGER.error("Radiation sidecar close failed", ex);
            }
            sidecarEpochs.clear();
            worldMap.clear();
            disabledAmbient.clear();
            RadiationDiffusivity.clear();
        }
        if (simulationFailure != null) throw simulationFailure;
    }

    public static byte @Nullable [] readSidecar(ResourceKey<Level> dimension, ChunkPos pos) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get()) return null;
        int cx = pos.x(), cz = pos.z();
        if (((cx ^ (cx << 10) >> 10) | (cz ^ (cz << 10) >> 10)) != 0) return null;
        MinecraftServer server = Services.SERVER.getCurrentServer();
        if (server == null) return null;
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return null;
        long before = sidecarEpochs.getOrDefault(dimension, 0L);
        try {
            byte[] payload = SIDE_CAR.read(level, pos);
            if (payload == null || sidecarEpochs.getOrDefault(dimension, 0L) != before) return null;
            return wrapSidecarCarry(before, payload);
        } catch (RuntimeException ex) {
            NuclearTech.LOGGER.error(
                    "Radiation sidecar read failed for chunk {} in dimension {}; ignoring it",
                    pos,
                    dimension.identifier(),
                    ex);
            discardSidecar(level, pos);
            return null;
        }
    }

    static void discardSidecar(ServerLevel level, ChunkPos pos) {
        try {
            SIDE_CAR.discard(level, pos);
        } catch (RuntimeException ex) {
            NuclearTech.LOGGER.error(
                    "Failed to discard invalid radiation sidecar for chunk {} in dimension {}",
                    pos,
                    level.dimension().identifier(),
                    ex);
        }
    }

    @ServerThread
    public static boolean jettisonData(ServerLevel world) {
        WorldRadiationData data = worldMap.get(world);
        awaitSimulation();
        try {
            SIDE_CAR.deleteDimension(world);
            sidecarEpochs.merge(world.dimension(), 1L, Long::sum);
        } catch (RuntimeException ex) {
            NuclearTech.LOGGER.error(
                    "Radiation sidecar delete failed for dimension {}",
                    world.dimension().identifier(),
                    ex);
            return false;
        }
        if (data == null) return true;
        data.pocketToDestroy = Long.MIN_VALUE;
        data.destructionQueue.clear(true);
        data.fogQueue.clear(true);
        data.clearQueuedRadiationDirty();
        data.clearAllChunkRefs();
        data.dirtyCk.clearAll();
        data.clearQueuedWrites();
        return true;
    }

    @ServerThread
    public static void incrementRad(ServerLevel world, BlockPos pos, double amount) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get()
                || Math.abs(amount) < RAD_EPSILON
                || isOutsideWorld(world, pos)) return;
        addOrEmit(world, pos, amount, 0.0D, false);
    }

    @ServerThread
    public static void incrementRad(
            ServerLevel world, BlockPos pos, double emission, double saturation) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get()
                || Math.abs(emission) < RAD_EPSILON
                || isOutsideWorld(world, pos)) return;
        addOrEmit(world, pos, emission, saturation, true);
    }

    private static void addOrEmit(
            ServerLevel world, BlockPos pos, double emission, double saturation, boolean isSource) {
        long posLong = pos.asLong();
        long sck = SectionPos.blockToSection(posLong);
        long ck = Library.sectionToChunkLong(sck);
        LevelChunk chunk = world.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
        if (chunk == null) return;
        if (isResistantAt(chunk, pos)) return;
        awaitSimulation();
        WorldRadiationData data = getWorldRadData(world);
        int ownerId = data.onChunkLoaded(chunk.getPos().x(), chunk.getPos().z(), chunk);
        int slot = data.slotOf(SectionPos.y(sck));
        if (slot < 0) return;
        if (data.backend != null) {
            if (isSource) data.backend.emitRad(pos, emission, saturation);
            else data.backend.addRad(pos, emission);
            return;
        }

        int local = Library.blockPosToLocal(posLong);
        if (isSource) data.queueEmit(sck, local, emission, saturation);
        else data.queueAdd(sck, local, emission);

        if (data.getKind(ownerId, slot) == KIND_NONE) data.dirtyCk.add(ck, ownerId, slot);
        data.setChunkDirty(ownerId);
    }

    @ServerThread
    public static void setRadForCoord(ServerLevel world, BlockPos pos, double amount) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get() || isOutsideWorld(world, pos)) return;
        long posLong = pos.asLong();
        long sck = SectionPos.blockToSection(posLong);
        long ck = Library.sectionToChunkLong(sck);
        LevelChunk chunk = world.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
        if (chunk == null) return;
        if (isResistantAt(chunk, pos)) return;
        awaitSimulation();
        WorldRadiationData data = getWorldRadData(world);
        int ownerId = data.onChunkLoaded(chunk.getPos().x(), chunk.getPos().z(), chunk);
        int slot = data.slotOf(SectionPos.y(sck));
        if (slot < 0) return;
        if (data.backend != null) {
            data.backend.setRad(pos, amount);
            return;
        }

        int local = Library.blockPosToLocal(posLong);
        data.queueSet(sck, local, amount);
        if (data.getKind(ownerId, slot) == KIND_NONE) data.dirtyCk.add(ck, ownerId, slot);
        data.setChunkDirty(ownerId);
    }

    public static double ambientRad(ServerLevel world) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get()) {
            return disabledAmbient.computeIfAbsent(
                    world, level -> RadiationSettings.forLevel(level).ambientRadOrDefault());
        }
        return getWorldRadData(world).ambientRad;
    }

    public static double doseAt(ServerLevel world, BlockPos pos) {
        double field = getRadForCoord(world, pos);
        double ambient = ambientRad(world);
        return field >= 0.0D ? Math.max(field, ambient) : Math.max(0.0D, field + ambient);
    }

    @ServerThread
    public static double getRadForCoord(ServerLevel world, BlockPos pos) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get() || isOutsideWorld(world, pos)) return 0D;
        long posLong = pos.asLong();
        long sck = SectionPos.blockToSection(posLong);
        long ck = Library.sectionToChunkLong(sck);
        LevelChunk chunk = world.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
        if (chunk == null) return 0D;
        awaitSimulation();
        WorldRadiationData data = worldMap.get(world);
        if (data == null) return 0D;
        if (isResistantAt(chunk, pos)) return 0D;
        int slot = data.slotOf(SectionPos.y(sck));
        if (slot < 0) return 0D;
        if (data.backend != null) return data.backend.getRad(pos);
        int ownerId = data.getId(ck);
        if (ownerId < 0) return 0D;
        if (ownerId >= data.cks.length || data.cks[ownerId] != ck) return 0D;

        int secIdx = (ownerId * data.sectionsPerChunk) + slot;
        int kind = data.getKind(ownerId, slot);

        if (kind == KIND_NONE) {
            data.dirtyCk.add(ck, ownerId, slot);
            return 0D;
        }
        if (kind == KIND_UNI) return data.uniformRads[secIdx];

        WorldRadiationData.SectionRef sc = data.complexSecs[secIdx];
        if (sc == null || sc.pocketCount <= 0) {
            data.dirtyCk.add(ck, ownerId, slot);
            return 0D;
        }
        int pocketIndex = sc.getPocketIndex(posLong);
        if (pocketIndex < 0) {
            data.dirtyCk.add(ck, ownerId, slot);
            return 0D;
        }
        if (kind == KIND_SINGLE) {
            return data.uniformRads[secIdx];
        } else {
            return ((WorldRadiationData.MultiSectionRef) sc).data[pocketIndex << 1];
        }
    }

    @ServerThread
    public static void markSectionForRebuild(Level world, BlockPos pos) {
        if (world == null || world.isClientSide() || !RadiationData.ENABLE_CHUNK_RADS.get()) return;
        if (!(world instanceof ServerLevel server)) return;
        if (isOutsideWorld(server, pos)) return;

        markSectionForRebuild(world, SectionPos.asLong(pos));
    }

    @ServerThread
    public static void markSectionForRebuild(Level world, long sck) {
        if (world == null || world.isClientSide() || !RadiationData.ENABLE_CHUNK_RADS.get()) return;
        if (!(world instanceof ServerLevel ws)) return;
        long ck = Library.sectionToChunkLong(sck);
        LevelChunk chunk = ws.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
        if (chunk == null) return;
        awaitSimulation();
        WorldRadiationData data = getWorldRadData(ws);
        int slot = data.slotOf(SectionPos.y(sck));
        if (slot < 0) return;
        int id = data.onChunkLoaded(chunk.getPos().x(), chunk.getPos().z(), chunk);
        data.dirtyCk.add(ck, id, slot);
        data.setChunkDirty(id);
    }

    @ServerThread
    public static void markSectionsForRebuild(Level world, LongIterable sections) {
        if (world == null || world.isClientSide() || !RadiationData.ENABLE_CHUNK_RADS.get()) return;
        if (!(world instanceof ServerLevel ws)) return;
        awaitSimulation();
        WorldRadiationData data = getWorldRadData(ws);
        LongIterator it = sections.iterator();
        while (it.hasNext()) {
            long sck = it.nextLong();
            long ck = Library.sectionToChunkLong(sck);
            LevelChunk chunk =
                    ws.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
            if (chunk == null) continue;

            int slot = data.slotOf(SectionPos.y(sck));
            if (slot < 0) continue;
            int id = data.onChunkLoaded(chunk.getPos().x(), chunk.getPos().z(), chunk);
            data.dirtyCk.add(ck, id, slot);
            data.setChunkDirty(id);
        }
    }

    static void runSweeps() {
        WorldRadiationData[] all = worldMap.values().toArray(new WorldRadiationData[0]);
        int n = all.length;
        if (n == 0) return;

        if (n == 1) {
            WorldRadiationData data = all[0];
            if (data.world.getServer() == null) return;
            try {
                data.runSweepPhases();
            } catch (Throwable t) {
                NuclearTech.LOGGER.error(
                        "Error in async rad simulation in dimension {}",
                        data.world.dimension().identifier(),
                        t);
            }
        } else {
            ForkJoinTask<?>[] tasks = new ForkJoinTask<?>[n];
            for (int i = 0; i < n; i++) {
                WorldRadiationData data = all[i];
                tasks[i] =
                        ForkJoinTask.adapt(
                                () -> {
                                    if (data.world.getServer() == null) return;
                                    try {
                                        data.runSweepPhases();
                                    } catch (Throwable t) {
                                        NuclearTech.LOGGER.error(
                                                "Error in async rad simulation in dimension {}",
                                                data.world.dimension().identifier(),
                                                t);
                                    }
                                });
            }
            ForkJoinTask.invokeAll(tasks);
        }
    }

    static void onChunkLoad(ServerLevel server, LevelChunk chunk) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get()) return;
        serverStopping = false;
        int cx = chunk.getPos().x(), cz = chunk.getPos().z();
        if (((cx ^ (cx << 10) >> 10) | (cz ^ (cz << 10) >> 10)) != 0) return;
        awaitSimulation();
        WorldRadiationData data = getWorldRadData(server);
        int id = data.onChunkLoaded(cx, cz, chunk);
        data.dirtyCk.add(ChunkPos.pack(cx, cz), id);

        byte[] sidecarBytes = chunk.hbm$getRadiation();
        if (sidecarBytes != null) {
            chunk.hbm$setRadiation(null);
            try {
                byte[] payload = unwrapSidecarCarry(server.dimension(), sidecarBytes);
                byte[] verified = payload == null ? null : verifyPayload(payload);
                if (verified != null) data.readPayload(cx, cz, verified);
            } catch (BufferUnderflowException | DecodeException ex) {
                NuclearTech.LOGGER.error(
                        "[RadiationSystemNT] Failed to decode data for chunk {} in dimension {}",
                        chunk.getPos(),
                        server.dimension().identifier(),
                        ex);
                discardSidecar(server, chunk.getPos());
            }
        }

        if (data.backend != null) {
            data.rebuildDirtySections();
            data.pushDensitiesToBackend(ChunkPos.pack(cx, cz), id);
            data.backend.chunkLoaded(chunk);
        }
    }

    static byte[] wrapSidecarCarry(long epoch, byte[] payload) {
        byte[] carry = new byte[SIDECAR_CARRY_HEADER_BYTES + payload.length];
        ByteBuffer.wrap(carry).putLong(epoch).put(payload);
        return carry;
    }

    static byte @Nullable [] unwrapSidecarCarry(ResourceKey<Level> dimension, byte[] carry)
            throws DecodeException {
        if (carry.length < SIDECAR_CARRY_HEADER_BYTES)
            throw new DecodeException("Invalid sidecar carrier");
        long epoch = ByteBuffer.wrap(carry).getLong();
        long current = sidecarEpochs.getOrDefault(dimension, 0L);
        if (epoch != current) return null;
        return Arrays.copyOfRange(carry, SIDECAR_CARRY_HEADER_BYTES, carry.length);
    }

    static void onChunkUnload(ServerLevel server, LevelChunk chunk) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get()) return;
        int cx = chunk.getPos().x(), cz = chunk.getPos().z();
        if (((cx ^ (cx << 10) >> 10) | (cz ^ (cz << 10) >> 10)) != 0) return;
        WorldRadiationData data = worldMap.get(server);
        if (data == null) return;
        if (serverStopping) return;
        awaitSimulation();
        data.saveRadiationDirty(chunk);
        data.unloadChunk(cx, cz);
        long ck = ChunkPos.pack(cx, cz);
        data.removeChunkRef(ck);

        if (data.backend != null) data.backend.chunkUnloaded(cx, cz);
    }

    static byte @Nullable [] verifyPayload(byte[] raw) throws DecodeException {
        if (raw.length == 0) return null;
        if (raw.length < 5) throw new DecodeException("Payload too short: " + raw.length);
        if (raw.length > FMT8_MAX_BYTES)
            throw new DecodeException("Payload too large: " + raw.length);
        if (raw[0] != MAGIC_0 || raw[1] != MAGIC_1 || raw[2] != MAGIC_2)
            throw new DecodeException("Invalid magic");
        byte fmt = raw[3];
        if (fmt != FMT) throw new DecodeException("Unknown format: " + fmt);
        return raw;
    }

    static @NonNull WorldRadiationData getWorldRadData(ServerLevel world) {
        return worldMap.computeIfAbsent(world, WorldRadiationData::new);
    }

    static boolean isResistantAt(LevelChunk chunk, BlockPos pos) {
        return RAD_RESISTANT_STATES.contains(chunk.getBlockState(pos));
    }

    @ServerThread
    public static void onBlockStateReplaced(LevelChunk chunk, BlockPos pos, BlockState next) {
        Level level = chunk.getLevel();
        if (level.isClientSide()) return;
        if (RAD_RESISTANT_STATES.isEmpty()
                && RAD_SOURCE_STATES.isEmpty()
                && RadiationDiffusivity.isTrivial()) return;
        BlockState prev = chunk.getBlockState(pos);
        if (prev == next) return;
        if (RAD_RESISTANT_STATES.contains(prev) != RAD_RESISTANT_STATES.contains(next)
                || !Objects.equals(RAD_SOURCE_STATES.get(prev), RAD_SOURCE_STATES.get(next))) {
            markSectionForRebuild(level, pos);
        } else if (RadiationDiffusivity.of(prev) != RadiationDiffusivity.of(next)) {
            markDiffusivityDirty((ServerLevel) level, pos);
        }
    }

    @ServerThread
    static void markDiffusivityDirty(ServerLevel level, BlockPos pos) {
        if (!RadiationData.ENABLE_CHUNK_RADS.get() || isOutsideWorld(level, pos)) return;
        WorldRadiationData data = worldMap.get(level);
        if (data != null && data.diffusivityTransport)
            data.diffusivityDirty.add(SectionPos.asLong(pos));
    }

    public static void markRadSource(
            Block block,
            ToDoubleFunction<BlockState> emission,
            ToDoubleFunction<BlockState> saturation) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            double e = emission.applyAsDouble(state);
            double s = saturation.applyAsDouble(state);

            if (e == 0.0D || !Double.isFinite(e) || !Double.isFinite(s)) continue;
            RAD_SOURCE_STATES.put(state, new RadSource(e, s));
        }
    }

    public static void markRadSource(Block block, double emission, double saturation) {
        markRadSource(block, state -> emission, state -> saturation);
    }

    public static void registerConstantSources(RegistryAccess registries) {
        int rules = 0;
        for (RadSourceRule rule : registries.lookupOrThrow(RadSourceRule.REGISTRY)) {
            for (Holder<Block> target : rule.targets()) {
                Block block = target.value();
                markRadSource(
                        block,
                        state -> rule.matches(state) ? rule.rate().emission(block) : 0.0D,
                        state -> rule.matches(state) ? rule.rate().saturation(block) : 0.0D);
            }
            rules++;
        }
        NuclearTech.LOGGER.info(
                "[RadiationSystemNT] {} constant-source rules, {} states resolved",
                rules,
                RAD_SOURCE_STATES.size());
    }

    record RadSource(double emission, double saturation) {}

    public static void markRadResistant(Block block) {
        boolean changed = false;
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            changed |= RAD_RESISTANT_STATES.add(state);
        }
        if (changed) resistantStateVersion++;
    }

    public static void markRadResistant(Block block, Predicate<BlockState> filter) {
        boolean changed = false;
        for (BlockState s : block.getStateDefinition().getPossibleStates()) {
            if (filter.test(s)) changed |= RAD_RESISTANT_STATES.add(s);
        }
        if (changed) resistantStateVersion++;
    }

    static boolean isOutsideWorld(ServerLevel world, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int bad = ((x << 6) >> 6) ^ x;
        bad |= ((z << 6) >> 6) ^ z;
        if (bad != 0) return true;
        return y < world.getMinY() || y > world.getMaxY();
    }

    static long pocketKey(long sectionKey, int pocketIndex, int slot) {
        int yz = (slot << 11) | pocketIndex;
        return Library.setSectionY(sectionKey, yz);
    }

    static @Nullable SectionMask scanResistantMask(@Nullable LevelChunkSection section) {
        if (section == null || section.hasOnlyAir()) return null;
        return scanResistantMask(section.getStates());
    }

    static @Nullable SectionMask scanResistantMask(PalettedContainer<BlockState> container) {
        if (RAD_RESISTANT_STATES.isEmpty()) return null;

        PalettedContainer.Data<BlockState> data = container.data;
        BitStorage storage = data.storage();
        Palette<BlockState> pal = data.palette();
        int bits = storage.getBits();

        if (bits == 0 || pal instanceof SingleValuePalette<?>) {
            return RAD_RESISTANT_STATES.contains(pal.valueFor(0)) ? SectionMask.ALL_SET : null;
        }

        long[] resistantIds;
        int idLimit;

        if (pal instanceof GlobalPalette<?>) {
            PalScratch sc = TL_PAL_SCRATCH.get();
            idLimit = Block.BLOCK_STATE_REGISTRY.size();
            resistantIds = sc.globalResistantIds(idLimit, resistantStateVersion);
        } else {
            idLimit = pal.getSize();
            if (idLimit > 256) {
                throw new IllegalStateException(
                        "Unexpected local block-state palette size: " + idLimit);
            }

            long ids0 = 0L, ids1 = 0L, ids2 = 0L, ids3 = 0L;
            int resistantCount = 0;
            for (int id = 0; id < idLimit; id++) {
                if (!RAD_RESISTANT_STATES.contains(pal.valueFor(id))) continue;
                long bit = 1L << (id & 63);
                switch (id >>> 6) {
                    case 0 -> ids0 |= bit;
                    case 1 -> ids1 |= bit;
                    case 2 -> ids2 |= bit;
                    default -> ids3 |= bit;
                }
                resistantCount++;
            }
            if (resistantCount == 0) return null;
            if (resistantCount == idLimit) return SectionMask.ALL_SET;

            if (bits == 4) return scanPacked4BitResistantIds(storage.getRaw(), ids0);
            PalScratch sc = TL_PAL_SCRATCH.get();
            long[] local = sc.localResistantIds;
            local[0] = ids0;
            local[1] = ids1;
            local[2] = ids2;
            local[3] = ids3;
            if (bits >= 5 && bits <= 7) {
                boolean[] localResistant = sc.localResistant;
                for (int id = 0; id < idLimit; id++) {
                    localResistant[id] = (local[id >>> 6] & (1L << (id & 63))) != 0L;
                }

                Arrays.fill(localResistant, idLimit, localResistant.length, false);
                return scanUnpackedResistantIds(storage, sc.unpacked, localResistant);
            }
            resistantIds = local;
        }

        return switch (bits) {
            case 4 -> scanPacked4BitResistantIds(storage.getRaw(), resistantIds[0]);
            case 5 -> scanPackedResistantIds(storage.getRaw(), 5, 12, 0x1FL, resistantIds, idLimit);
            case 6 -> scanPackedResistantIds(storage.getRaw(), 6, 10, 0x3FL, resistantIds, idLimit);
            case 7 -> scanPackedResistantIds(storage.getRaw(), 7, 9, 0x7FL, resistantIds, idLimit);
            case 8 -> scanPacked8BitResistantIds(storage.getRaw(), resistantIds, idLimit);
            default ->
                    scanPackedResistantIds(
                            storage.getRaw(),
                            bits,
                            64 / bits,
                            (1L << bits) - 1L,
                            resistantIds,
                            idLimit);
        };
    }

    static final class SectionSources {
        short[] pocket = new short[4];
        double[] emission = new double[4];
        double[] saturation = new double[4];
        int[] count = new int[4];
        int size;

        void add(int p, RadSource source, int[] pocketVolumes) {
            double scale =
                    (double) SectionPos.SECTION_BLOCK_COUNT
                            / (double) Math.max(1, pocketVolumes[p]);
            double e = source.emission() * scale;
            double s = source.saturation() == 0.0D ? 0.0D : source.saturation() * scale;
            add(p, e, s);
        }

        void add(int p, double e, double s) {
            for (int i = 0; i < size; i++) {
                if (pocket[i] == p && emission[i] == e && saturation[i] == s) {
                    count[i]++;
                    return;
                }
            }
            if (size == pocket.length) {
                int n = size * 2;
                pocket = Arrays.copyOf(pocket, n);
                emission = Arrays.copyOf(emission, n);
                saturation = Arrays.copyOf(saturation, n);
                count = Arrays.copyOf(count, n);
            }
            pocket[size] = (short) p;
            emission[size] = e;
            saturation[size] = s;
            count[size] = 1;
            size++;
        }

        double relax(int p, double c) {
            double weight = 0.0D;
            double numerator = 0.0D;
            double add = 0.0D;
            for (int i = 0; i < size; i++) {
                if (pocket[i] != p) continue;
                double e = emission[i];
                double sat = saturation[i];
                if (sat == 0.0D) {
                    add += count[i] * e;
                    continue;
                }
                if (!Emission.active(e, sat, c)) continue;
                weight += count[i] * Emission.weight(e, sat);
                numerator += count[i] * Emission.numeratorTerm(e, sat);
            }
            return Emission.relax(c, weight, numerator) + add;
        }
    }

    static @Nullable SectionSources scanSectionSources(
            @Nullable LevelChunkSection section,
            short @Nullable [] pocketData,
            int pocketCount,
            int[] pocketVolumes) {
        if (RAD_SOURCE_STATES.isEmpty() || section == null || section.hasOnlyAir()) return null;
        PalettedContainer<BlockState> container = section.getStates();
        PalettedContainer.Data<BlockState> data = container.data;
        Palette<BlockState> pal = data.palette();
        BitStorage storage = data.storage();

        if (storage.getBits() == 0 || pal instanceof SingleValuePalette<?>) {
            RadSource single = RAD_SOURCE_STATES.get(pal.valueFor(0));
            if (single == null) return null;
            SectionSources out = new SectionSources();
            for (int local = 0; local < SectionPos.SECTION_BLOCK_COUNT; local++) {
                int pi = pocketData == null ? 0 : pocketData[local];
                if (pi < 0 || pi >= pocketCount) continue;
                out.add(pi, single, pocketVolumes);
            }
            return out.size == 0 ? null : out;
        }

        int idLimit =
                pal instanceof GlobalPalette<?> ? Block.BLOCK_STATE_REGISTRY.size() : pal.getSize();
        RadSource[] byId = null;
        for (int id = 0; id < idLimit; id++) {
            BlockState state = pal.valueFor(id);
            RadSource source = state == null ? null : RAD_SOURCE_STATES.get(state);
            if (source == null) continue;
            if (byId == null) byId = new RadSource[idLimit];
            byId[id] = source;
        }
        if (byId == null) return null;

        SectionSources out = new SectionSources();
        for (int local = 0; local < SectionPos.SECTION_BLOCK_COUNT; local++) {
            int id = storage.get(local);
            if (id < 0 || id >= idLimit) continue;
            RadSource source = byId[id];
            if (source == null) continue;
            int pi = pocketData == null ? 0 : pocketData[local];
            if (pi < 0 || pi >= pocketCount) continue;
            out.add(pi, source, pocketVolumes);
        }
        return out.size == 0 ? null : out;
    }

    static float scanSectionDiffusivity(
            @Nullable LevelChunkSection section,
            short @Nullable [] pocketData,
            int pocketCount,
            int[] pocketVolumes,
            float @Nullable [] out) {
        if (pocketCount == 0) return RadiationDiffusivity.NEUTRAL;
        if (RadiationDiffusivity.isTrivial() || section == null || section.hasOnlyAir()) {
            if (out != null) Arrays.fill(out, 0, pocketCount, RadiationDiffusivity.NEUTRAL);
            return RadiationDiffusivity.NEUTRAL;
        }
        PalettedContainer<BlockState> container = section.getStates();
        PalettedContainer.Data<BlockState> data = container.data;
        Palette<BlockState> pal = data.palette();
        BitStorage storage = data.storage();

        if (storage.getBits() == 0 || pal instanceof SingleValuePalette<?>) {
            float d = RadiationDiffusivity.of(pal.valueFor(0));
            if (d == RadiationDiffusivity.NEUTRAL) {
                if (out != null) Arrays.fill(out, 0, pocketCount, RadiationDiffusivity.NEUTRAL);
                return RadiationDiffusivity.NEUTRAL;
            }
            d = RadiationDiffusivity.clamp(d);
            if (out != null) {
                for (int p = 0; p < pocketCount; p++) {
                    out[p] =
                            pocketData == null || pocketVolumes[p] > 0
                                    ? d
                                    : RadiationDiffusivity.NEUTRAL;
                }
            }
            return pocketData == null || pocketVolumes[0] > 0 ? d : RadiationDiffusivity.NEUTRAL;
        }

        PalScratch sc = TL_PAL_SCRATCH.get();
        int idLimit;
        double[] reciprocalById;
        if (pal instanceof GlobalPalette<?>) {

            idLimit = Block.BLOCK_STATE_REGISTRY.size();
            reciprocalById =
                    sc.globalDiffusivityReciprocal(idLimit, RadiationDiffusivity.stateVersion);
            if (reciprocalById == null) {
                if (out != null) Arrays.fill(out, 0, pocketCount, RadiationDiffusivity.NEUTRAL);
                return RadiationDiffusivity.NEUTRAL;
            }
        } else {
            idLimit = pal.getSize();
            if (idLimit > 256) {
                throw new IllegalStateException(
                        "Unexpected local block-state palette size: " + idLimit);
            }
            reciprocalById = sc.localDiffusivityReciprocal;
            boolean any = false;
            for (int id = 0; id < idLimit; id++) {
                BlockState state = pal.valueFor(id);
                float d =
                        state == null
                                ? RadiationDiffusivity.NEUTRAL
                                : RadiationDiffusivity.of(state);
                reciprocalById[id] = 1.0D / d;
                any |= d != RadiationDiffusivity.NEUTRAL;
            }
            if (!any) {
                if (out != null) Arrays.fill(out, 0, pocketCount, RadiationDiffusivity.NEUTRAL);
                return RadiationDiffusivity.NEUTRAL;
            }
        }

        return harmonicByPocket(
                pocketData, pocketCount, pocketVolumes, storage, reciprocalById, idLimit, out);
    }

    private static float harmonicByPocket(
            short @Nullable [] pocketData,
            int pocketCount,
            int[] pocketVolumes,
            BitStorage storage,
            double[] reciprocalById,
            int idLimit,
            float @Nullable [] out) {
        if (pocketData == null) {
            double reciprocal = 0.0D;
            for (int local = 0; local < SectionPos.SECTION_BLOCK_COUNT; local++) {
                int id = storage.get(local);
                reciprocal += (id < 0 || id >= idLimit) ? 1.0D : reciprocalById[id];
            }
            float value =
                    RadiationDiffusivity.clamp(
                            (float) (SectionPos.SECTION_BLOCK_COUNT / reciprocal));
            if (out != null) {
                Arrays.fill(out, 0, pocketCount, RadiationDiffusivity.NEUTRAL);
                out[0] = value;
            }
            return value;
        }
        double[] recip = TL_DIFF_RECIPROCALS.get();
        Arrays.fill(recip, 0, pocketCount, 0.0D);
        for (int local = 0; local < SectionPos.SECTION_BLOCK_COUNT; local++) {
            int pi = pocketData[local];
            if (pi < 0 || pi >= pocketCount) continue;
            int id = storage.get(local);
            recip[pi] += (id < 0 || id >= idLimit) ? 1.0D : reciprocalById[id];
        }
        float first = RadiationDiffusivity.NEUTRAL;
        for (int p = 0; p < pocketCount; p++) {

            float value;
            if (!(recip[p] > 0.0D)) {
                value = RadiationDiffusivity.NEUTRAL;
            } else {
                value = RadiationDiffusivity.clamp((float) (pocketVolumes[p] / recip[p]));
            }
            if (out != null) out[p] = value;
            if (p == 0) first = value;
        }
        return first;
    }

    static @Nullable SectionMask scanPacked4BitResistantIds(long[] packedWords, long resistantIds) {
        SectionMask result = null;
        int input = 0;
        for (int output = 0; output < 64; output++) {
            long outputWord = 0L;
            for (int group = 0; group < 4; group++) {
                long packed = packedWords[input++];
                long groupBits = 0L;
                for (int lane = 0; lane < 16; lane++) {
                    groupBits |= (resistantIds >>> (packed & 0xFL) & 1L) << lane;
                    packed >>>= 4;
                }
                outputWord |= groupBits << (group << 4);
            }
            if (outputWord == 0L) continue;
            if (result == null) result = new SectionMask();
            result.words[output] = outputWord;
        }
        return result;
    }

    static @Nullable SectionMask scanUnpackedResistantIds(
            BitStorage storage, int[] unpacked, boolean[] resistantIds) {
        storage.unpack(unpacked);
        SectionMask result = null;
        for (int output = 0; output < 64; output++) {
            int base = output << 6;
            long outputWord = 0L;
            for (int bit = 0; bit < 64; bit++) {
                if (resistantIds[unpacked[base + bit]]) outputWord |= 1L << bit;
            }
            if (outputWord == 0L) continue;
            if (result == null) result = new SectionMask();
            result.words[output] = outputWord;
        }
        return result;
    }

    static @Nullable SectionMask scanPacked8BitResistantIds(
            long[] packedWords, long[] resistantIds, int idLimit) {
        SectionMask result = null;
        int input = 0;
        for (int output = 0; output < 64; output++) {
            long outputWord = 0L;
            for (int group = 0; group < 8; group++) {
                long packed = packedWords[input++];
                long groupBits = 0L;
                for (int lane = 0; lane < 8; lane++) {
                    int id = (int) (packed & 0xFFL);
                    if (id < idLimit && (resistantIds[id >>> 6] & (1L << (id & 63))) != 0L) {
                        groupBits |= 1L << lane;
                    }
                    packed >>>= 8;
                }
                outputWord |= groupBits << (group << 3);
            }
            if (outputWord == 0L) continue;
            if (result == null) result = new SectionMask();
            result.words[output] = outputWord;
        }
        return result;
    }

    static @Nullable SectionMask scanPackedResistantIds(
            long[] packedWords,
            int bits,
            int valuesPerLong,
            long valueMask,
            long[] resistantIds,
            int idLimit) {
        SectionMask result = null;
        int index = 0;
        int outputIndex = 0;
        int outputBit = 0;
        long outputWord = 0L;
        for (long packed : packedWords) {
            int count = Math.min(valuesPerLong, SectionPos.SECTION_BLOCK_COUNT - index);
            for (int i = 0; i < count; i++) {
                int id = (int) (packed & valueMask);
                if (id >= 0 && id < idLimit && (resistantIds[id >>> 6] & (1L << (id & 63))) != 0L) {
                    outputWord |= 1L << outputBit;
                }
                packed >>>= bits;
                index++;
                if (++outputBit == 64) {
                    if (outputWord != 0L) {
                        if (result == null) result = new SectionMask();
                        result.words[outputIndex] = outputWord;
                    }
                    outputIndex++;
                    outputBit = 0;
                    outputWord = 0L;
                }
            }
        }
        return result;
    }

    static double sanitize(double v, double minBound) {
        if (Double.isNaN(v) || Math.abs(v) < RAD_EPSILON && v > minBound) return 0.0D;
        return Math.max(Math.min(v, RAD_MAX), minBound);
    }

    static boolean exchangeUniExactXZ(double[] uni, int idxA, int idxB, double uuE) {
        double ra = uni[idxA], rb = uni[idxB];
        if (ra == rb) return false;
        double avg = 0.5d * (ra + rb);
        double halfDiff = 0.5d * (ra - rb);
        uni[idxA] = Math.fma(halfDiff, uuE, avg);
        uni[idxB] = Math.fma(-halfDiff, uuE, avg);
        return true;
    }

    static boolean exchangeUniExactY(double[] uni, int idxA, int idxB, double uuE) {
        double ra = uni[idxA];
        double rb = uni[idxB];
        if (ra == rb) return false;
        double avg = 0.5d * (ra + rb);
        double halfDiff = 0.5d * (ra - rb);
        uni[idxA] = Math.fma(halfDiff, uuE, avg);
        uni[idxB] = Math.fma(-halfDiff, uuE, avg);
        return true;
    }

    static boolean exchangeUniExactDiffusive(
            double[] uni,
            int idxA,
            int idxB,
            double uuE,
            double diffusionDt,
            float diffA,
            float diffB) {
        double dEff = RadiationDiffusivity.edge(8.0d, diffA, 8.0d, diffB);
        if (!(dEff > 0.0d)) return false;
        double e = dEff == 1.0d ? uuE : RadiationDiffusivity.decay((diffusionDt / 128.0d) * dEff);
        return exchangeUniExactXZ(uni, idxA, idxB, e);
    }

    static final class PendingRad {
        final SavedSection[] bySy;
        int nonEmptySections;

        PendingRad(int sectionsPerChunk) {
            this.bySy = new SavedSection[sectionsPerChunk];
        }

        boolean hasSy(int sy) {
            return bySy[sy] != null;
        }

        boolean isEmpty() {
            return nonEmptySections == 0;
        }

        void clearAll() {
            Arrays.fill(bySy, null);
            nonEmptySections = 0;
        }

        void clearSy(int sy) {
            if (bySy[sy] != null) {
                bySy[sy] = null;
                nonEmptySections--;
            }
        }

        void put(int sy, SavedSection section) {
            if (bySy[sy] == null) nonEmptySections++;
            bySy[sy] = section;
        }

        SavedSection get(int sy) {
            return bySy[sy];
        }

        SavedSection take(int sy) {
            SavedSection section = bySy[sy];
            clearSy(sy);
            return section;
        }

        record SavedSection(
                int pocketCount, short @Nullable [] pocketData, int[] volumes, double[] densities) {

            boolean hasSameTopology(int currentPocketCount, short @Nullable [] currentPocketData) {
                return pocketCount == currentPocketCount
                        && Arrays.equals(pocketData, currentPocketData);
            }
        }
    }

    static final class PalScratch {
        final long[] localResistantIds = new long[4];
        final boolean[] localResistant = new boolean[256];
        final double[] localDiffusivityReciprocal = new double[256];
        final int[] unpacked = new int[SectionPos.SECTION_BLOCK_COUNT];
        long[] globalResistantIds = new long[0];
        int globalRegistrySize = -1;
        int globalVersion = -1;
        double @Nullable [] globalDiffusivityReciprocal;
        int globalDiffusivitySize = -1;
        int globalDiffusivityVersion = -1;

        double @Nullable [] globalDiffusivityReciprocal(int registrySize, int version) {
            if (globalDiffusivitySize == registrySize && globalDiffusivityVersion == version) {
                return globalDiffusivityReciprocal;
            }
            double[] reciprocal = globalDiffusivityReciprocal;
            if (reciprocal == null || reciprocal.length < registrySize)
                reciprocal = new double[registrySize];
            Arrays.fill(reciprocal, 0, registrySize, 1.0D);
            boolean any = false;
            for (int id = 0; id < registrySize; id++) {
                BlockState state = Block.BLOCK_STATE_REGISTRY.byId(id);
                if (state == null) continue;
                float d = RadiationDiffusivity.of(state);
                if (d == RadiationDiffusivity.NEUTRAL) continue;
                reciprocal[id] = 1.0D / d;
                any = true;
            }
            globalDiffusivityReciprocal = any ? reciprocal : null;
            globalDiffusivitySize = registrySize;
            globalDiffusivityVersion = version;
            return globalDiffusivityReciprocal;
        }

        long[] globalResistantIds(int registrySize, int version) {
            if (globalRegistrySize == registrySize && globalVersion == version)
                return globalResistantIds;
            int words = (registrySize + 63) >>> 6;
            if (globalResistantIds.length < words) globalResistantIds = new long[words];
            else Arrays.fill(globalResistantIds, 0, words, 0L);
            for (BlockState state : RAD_RESISTANT_STATES) {
                int id = Block.BLOCK_STATE_REGISTRY.getId(state);
                if (id >= 0 && id < registrySize) globalResistantIds[id >>> 6] |= 1L << (id & 63);
            }
            globalRegistrySize = registrySize;
            globalVersion = version;
            return globalResistantIds;
        }
    }

    static final class SectionMask {
        static final SectionMask ALL_SET = new SectionMask();

        static {
            Arrays.fill(ALL_SET.words, -1L);
        }

        final long[] words = new long[64];

        boolean get(int bit) {
            int w = bit >>> 6;
            return (words[w] & (1L << (bit & 63))) != 0L;
        }

        void set(int bit) {
            int w = bit >>> 6;
            words[w] |= (1L << (bit & 63));
        }

        boolean isEmpty() {
            for (long w : words) if (w != 0L) return false;
            return true;
        }
    }
}

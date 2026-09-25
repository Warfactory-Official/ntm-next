// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.handler.threading.BombForkJoinPool;
import com.hbm.interfaces.BitMask;
import com.hbm.interfaces.ServerThread;
import com.hbm.interfaces.ThreadSafeMethod;
import com.hbm.interfaces.injected.IBulkLightEngine;
import com.hbm.lib.Library;
import com.hbm.lib.internal.UnsafeHolder;
import com.hbm.lib.maps.NonBlockingHashMapLong;
import com.hbm.packet.toclient.OcclusionRefreshPayload;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class ChunkUtil {

    private static final Object2IntOpenHashMap<Identifier> activeTask =
            new Object2IntOpenHashMap<>();

    private static final Map<Identifier, NonBlockingHashMapLong<LevelChunk>> chunkMap =
            new ConcurrentHashMap<>();
    private static final BlockState AIR_DEFAULT_STATE = Blocks.AIR.defaultBlockState();
    private static final int DENSE_THRESHOLD = 4096 / 3;
    private static final ThreadLocal<Int2ObjectOpenHashMap<BlockState>[]> TL_BUCKET =
            ThreadLocal.withInitial(
                    () -> {
                        @SuppressWarnings("unchecked")
                        Int2ObjectOpenHashMap<BlockState>[] maps = new Int2ObjectOpenHashMap[24];
                        for (int i = 0; i < maps.length; i++)
                            maps[i] = new Int2ObjectOpenHashMap<>();
                        return maps;
                    });
    private static final ThreadLocal<BlockState[]> TL_OVERRIDES =
            ThreadLocal.withInitial(() -> new BlockState[4096]);

    private static final ThreadLocal<int[]> TL_CARVED =
            ThreadLocal.withInitial(() -> new int[4096]);

    public static RegistryHandle<TicketType> BLAST_LOAD;

    public static RegistryHandle<TicketType> ENTITY_HOLD;
    public static final int HOLD_RADIUS = ServerPlayer.ENDER_PEARL_TICKET_RADIUS;
    private static volatile int refCounter = 0;
    private static final Map<ServerLevel, Long2IntOpenHashMap> BLAST_LOAD_CLAIMS =
            new WeakHashMap<>();

    private ChunkUtil() {}

    public static void init() {
        Services.SERVER.onChunkLoad(ChunkUtil::onChunkLoad);
        Services.SERVER.onChunkUnload(ChunkUtil::onChunkUnload);

        Services.SERVER.onServerStopping(
                server -> {
                    BombForkJoinPool.onServerStopped();
                    onServerStopped();
                });
    }

    @ServerThread
    public static NonBlockingHashMapLong<LevelChunk> acquireMirrorMap(ServerLevel level) {
        Identifier dim = level.dimension().identifier();
        NonBlockingHashMapLong<LevelChunk> thisDim;
        if (activeTask.addTo(dim, 1) == 0) {
            thisDim = new NonBlockingHashMapLong<>(4096);
            ServerChunkCache cache = level.getChunkSource();
            ChunkMap cm = cache.chunkMap;

            for (ChunkHolder holder : cm.visibleChunkMap.values()) {
                LevelChunk chunk = fullChunkIfLoaded(holder);
                if (chunk != null) thisDim.put(chunk.getPos().pack(), chunk);
            }
            chunkMap.put(dim, thisDim);
        } else {
            thisDim = chunkMap.get(dim);
        }
        refCounter++;
        return Objects.requireNonNull(thisDim);
    }

    @ServerThread
    private static @Nullable LevelChunk fullChunkIfLoaded(@Nullable ChunkHolder holder) {
        if (holder == null || holder.getTicketLevel() > ChunkLevel.byStatus(FullChunkStatus.FULL))
            return null;
        return holder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).orElse(null);
    }

    @ServerThread
    public static @Nullable LevelChunk liveChunkNow(ServerLevel level, long chunkPos) {
        return fullChunkIfLoaded(
                level.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunkPos));
    }

    @ServerThread
    public static void forEachLoadedChunk(ServerLevel level, Consumer<LevelChunk> visitor) {
        for (ChunkHolder holder : level.getChunkSource().chunkMap.visibleChunkMap.values()) {
            LevelChunk chunk = fullChunkIfLoaded(holder);
            if (chunk != null) visitor.accept(chunk);
        }
    }

    @ServerThread
    public static void releaseMirrorMap(ServerLevel level) {
        Identifier dim = level.dimension().identifier();
        if (activeTask.addTo(dim, -1) == 1) {
            chunkMap.remove(dim);
            activeTask.removeInt(dim);
        }
        refCounter--;
    }

    public static void register(IRegistrar registrar) {
        BLAST_LOAD =
                registrar.registerTicketType(
                        "blast_load",
                        () -> new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));
        ENTITY_HOLD =
                registrar.registerTicketType(
                        "entity_hold",
                        () ->
                                new TicketType(
                                        40L,
                                        TicketType.FLAG_PERSIST
                                                | TicketType.FLAG_LOADING
                                                | TicketType.FLAG_SIMULATION
                                                | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    }

    @ServerThread
    public static void holdForEntity(ServerLevel level, ChunkPos chunkPos, int radius) {
        level.getChunkSource().addTicketWithRadius(ENTITY_HOLD.get(), chunkPos, radius);
    }

    public static void holdOwnChunk(Entity entity) {
        if (entity.level() instanceof ServerLevel server)
            holdForEntity(server, entity.chunkPosition(), HOLD_RADIUS);
    }

    @ServerThread
    public static void loadForEntity(ServerLevel level, ChunkPos chunkPos) {
        holdForEntity(level, chunkPos, HOLD_RADIUS);
        level.getChunk(chunkPos.x(), chunkPos.z());
    }

    @ServerThread
    public static void loadTickingForEntity(ServerLevel level, ChunkPos pos) {
        holdForEntity(level, pos, HOLD_RADIUS);
        level.getChunk(pos.x(), pos.z());
        ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(pos.pack());
        CompletableFuture<ChunkResult<LevelChunk>> ticking = holder.getEntityTickingChunkFuture();
        level.getServer().managedBlock(ticking::isDone);
        ticking.join().orElseThrow(IllegalStateException::new);
        level.waitForEntities(pos, 0);
    }

    @ServerThread
    public static void addBlastTicket(ServerLevel level, long chunkPos) {
        Long2IntOpenHashMap held =
                BLAST_LOAD_CLAIMS.computeIfAbsent(level, ignored -> new Long2IntOpenHashMap());

        if (held.addTo(chunkPos, 1) > 0) return;
        ChunkPos pos = new ChunkPos(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
        level.getChunkSource().addTicketWithRadius(BLAST_LOAD.get(), pos, 0);
    }

    @ServerThread
    public static void flushChunkTickets(ServerLevel level) {
        level.getChunkSource().runDistanceManagerUpdates();
    }

    @ServerThread
    public static @Nullable CompletableFuture<ChunkResult<LevelChunk>> blastChunkFuture(
            ServerLevel level, long chunkPos) {
        ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunkPos);
        return holder == null ? null : holder.getFullChunkFuture();
    }

    @ServerThread
    public static void releaseChunkAsync(ServerLevel level, long chunkPos) {
        Long2IntOpenHashMap held = BLAST_LOAD_CLAIMS.get(level);
        int count = held.addTo(chunkPos, -1);
        assert count > 0;
        if (count > 1) return;
        held.remove(chunkPos);
        if (held.isEmpty()) BLAST_LOAD_CLAIMS.remove(level);
        ChunkPos pos = new ChunkPos(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
        level.getChunkSource().removeTicketWithRadius(BLAST_LOAD.get(), pos, 0);
    }

    public static @Nullable LevelChunk chunkIfLoaded(Level level, BlockPos pos) {
        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());
        return level instanceof ServerLevel server
                ? server.getChunkSource().getChunkNow(cx, cz)
                : level.getChunkSource().getChunk(cx, cz, false);
    }

    public static @Nullable BlockEntity blockEntityIfLoaded(Level level, BlockPos pos) {
        LevelChunk chunk = chunkIfLoaded(level, pos);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }

    public static <T extends BlockEntity> @Nullable T blockEntityIfLoaded(
            Class<T> type, Level level, BlockPos pos) {
        BlockEntity be = blockEntityIfLoaded(level, pos);
        return type.isInstance(be) ? type.cast(be) : null;
    }

    public static @Nullable BlockState blockStateIfLoaded(Level level, BlockPos pos) {
        LevelChunk chunk = chunkIfLoaded(level, pos);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    @ThreadSafeMethod
    public static @Nullable LevelChunk getLoadedChunk(
            NonBlockingHashMapLong<LevelChunk> mirror, long chunkPos) {
        return mirror.get(chunkPos);
    }

    @ThreadSafeMethod
    public static LevelChunkSection @Nullable [] getLoadedSections(
            NonBlockingHashMapLong<LevelChunk> mirror, long chunkPos) {
        LevelChunk chunk = getLoadedChunk(mirror, chunkPos);
        return chunk == null ? null : chunk.getSections();
    }

    @ServerThread
    public static boolean postCarveBlockUpdate(
            ServerLevel level,
            LevelChunk chunk,
            BlockPos pos,
            BlockState oldState,
            BlockState newState) {
        dispatchRemovalHook(level, pos, oldState, newState);

        newState.onPlace(level, pos, oldState, false);
        int lx = pos.getX() & 15;
        int ly = pos.getY();
        int lz = pos.getZ() & 15;
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING)
                .update(lx, ly, lz, newState);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)
                .update(lx, ly, lz, newState);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR)
                .update(lx, ly, lz, newState);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE)
                .update(lx, ly, lz, newState);
        return Services.PLATFORM.hasDifferentLightProperties(chunk, pos, oldState, newState);
    }

    @ServerThread
    public static void replaceEmptied(
            ServerLevel level, BlockPos pos, BlockState state, @Block.UpdateFlags int flags) {
        BlockPos at = pos.immutable();
        if (level.getBlockState(at).hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(at);

            if (blockEntity instanceof RandomizableContainer loot) loot.setLootTable(null);
            if (blockEntity instanceof Container container) container.clearContent();
        }
        level.setBlock(at, state, flags);
    }

    @ServerThread
    public static void dispatchRemovalHook(
            ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        Block newBlock = newState.getBlock();
        if (!oldState.is(newBlock) || newBlock instanceof BaseRailBlock) {
            oldState.affectNeighborsAfterRemoval(level, pos, false);
        }
    }

    @ServerThread
    public static CompletableFuture<?> updateLight(
            ServerLevel level, LevelChunk chunk, LongList positions, long sectionMaskFromBottom) {
        assert level.getServer().isSameThread();
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        if (lightEngine instanceof IBulkLightEngine bulk) {
            return bulk.hbm$updateLight(chunk, positions, sectionMaskFromBottom);
        }
        return updateLightIndividually(level, chunk, positions, sectionMaskFromBottom);
    }

    @ServerThread
    public static CompletableFuture<?> updateLightIndividually(
            ServerLevel level, LevelChunk chunk, LongList positions, long sectionMaskFromBottom) {
        assert level.getServer().isSameThread();
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        int cx = chunk.getPos().x();
        int cz = chunk.getPos().z();
        int minSectionY = level.getMinSectionY();
        LevelChunkSection[] sections = chunk.getSections();
        for (long mask = sectionMaskFromBottom; mask != 0; mask &= mask - 1) {
            int subY = Long.numberOfTrailingZeros(mask);
            lightEngine.updateSectionStatus(
                    SectionPos.of(cx, subY + minSectionY, cz), sections[subY].hasOnlyAir());
        }
        var skyLightSources = chunk.getSkyLightSources();
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = BlockPos.of(positions.getLong(i));
            skyLightSources.update(chunk, pos.getX() & 15, pos.getY(), pos.getZ() & 15);
            lightEngine.checkBlock(pos);
        }
        return lightEngine.waitForPendingTasks(cx, cz);
    }

    @ServerThread
    public static void broadcastWholesaleResend(
            ServerLevel level, long chunkPos, CompletableFuture<?> lighting) {
        int cx = ChunkPos.getX(chunkPos);
        int cz = ChunkPos.getZ(chunkPos);
        MinecraftServer server = level.getServer();
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        lighting.thenAcceptAsync(
                ignored -> {
                    LevelChunk fresh = level.getChunkSource().getChunkNow(cx, cz);
                    if (fresh == null) return;
                    fresh.markUnsaved();
                    List<ServerPlayer> players =
                            level.getChunkSource().chunkMap.getPlayers(fresh.getPos(), false);
                    if (players.isEmpty()) return;
                    ClientboundLevelChunkWithLightPacket chunkPacket =
                            new ClientboundLevelChunkWithLightPacket(
                                    fresh, lightEngine, null, null);
                    ClientboundCustomPayloadPacket propagatePacket =
                            new ClientboundCustomPayloadPacket(
                                    new OcclusionRefreshPayload(chunkPos));
                    for (ServerPlayer p : players) {
                        p.connection.send(chunkPacket);
                        p.connection.send(propagatePacket);
                    }
                },
                server);
    }

    @ThreadSafeMethod
    @Contract(mutates = "param7,param8")
    public static @Nullable LevelChunkSection copyAndCarveLocal(
            ServerLevel level,
            int chunkX,
            int chunkZ,
            int subY,
            LevelChunkSection @NotNull [] sections,
            BitMask localMask,
            @Nullable LongCollection edgeOut,
            @Nullable Long2ObjectMap<BlockState> modifiedOut,
            boolean skipBlockEntities) {
        LevelChunkSection src = getSectionVolatile(sections, subY);
        return carveLocal(
                level,
                chunkX,
                chunkZ,
                subY,
                src,
                localMask,
                edgeOut,
                modifiedOut,
                skipBlockEntities,
                false);
    }

    @ThreadSafeMethod
    public static boolean carveSnapshot(
            ServerLevel level,
            int chunkX,
            int chunkZ,
            int subY,
            SectionSnapshot snapshot,
            BitMask localMask,
            LongCollection edgeOut,
            Long2ObjectMap<BlockState> modifiedOut) {
        LevelChunkSection result =
                carveLocal(
                        level,
                        chunkX,
                        chunkZ,
                        subY,
                        snapshot.sections[subY],
                        localMask,
                        edgeOut,
                        modifiedOut,
                        true,
                        true);
        if (result == null) return false;
        snapshot.sections[subY] = result;
        snapshot.changed(subY);
        return true;
    }

    private static @Nullable LevelChunkSection carveLocal(
            ServerLevel level,
            int chunkX,
            int chunkZ,
            int subY,
            @Nullable LevelChunkSection src,
            BitMask localMask,
            @Nullable LongCollection edgeOut,
            @Nullable Long2ObjectMap<BlockState> modifiedOut,
            boolean skipBlockEntities,
            boolean owned) {
        if (src == null || src.hasOnlyAir()) return null;

        int nonEmpty = src.nonEmptyBlockCount;
        NonBlockingHashMapLong<LevelChunk> loaded = chunkMap.get(level.dimension().identifier());

        int xBase = chunkX << 4;
        int yBase = level.getMinY() + (subY << 4);
        int zBase = chunkZ << 4;

        int[] carved = TL_CARVED.get();
        int carvedCount = 0;
        for (int idx = localMask.nextSetBit(0);
                idx >= 0 && idx < 4096;
                idx = localMask.nextSetBit(idx + 1)) {
            int xLocal = Library.getLocalX(idx);
            int yLocal = Library.getLocalY(idx);
            int zLocal = Library.getLocalZ(idx);

            BlockState old = src.getBlockState(xLocal, yLocal, zLocal);
            if (old.isAir()) continue;

            long packedPos = BlockPos.asLong(xBase | xLocal, yBase | yLocal, zBase | zLocal);
            if (modifiedOut != null) modifiedOut.put(packedPos, old);

            if (skipBlockEntities && old.hasBlockEntity()) continue;

            if (edgeOut != null
                    && touchesEdge(
                            loaded, chunkX, chunkZ, subY, src, xLocal, yLocal, zLocal, localMask)) {
                edgeOut.add(packedPos);
            }
            carved[carvedCount++] = idx;
        }
        if (carvedCount == 0) return null;

        if (carvedCount == nonEmpty) {
            return new LevelChunkSection(
                    level.palettedContainerFactory().createForBlockStates(),
                    owned ? src.getBiomes() : src.getBiomes().copy());
        }

        LevelChunkSection dst = owned ? src : src.copy();
        for (int i = 0; i < carvedCount; i++) {
            int idx = carved[i];
            dst.setBlockState(
                    Library.getLocalX(idx),
                    Library.getLocalY(idx),
                    Library.getLocalZ(idx),
                    AIR_DEFAULT_STATE,
                    false);
        }
        return dst;
    }

    private static boolean touchesEdge(
            @Nullable NonBlockingHashMapLong<LevelChunk> loaded,
            int chunkX,
            int chunkZ,
            int subY,
            LevelChunkSection src,
            int xLocal,
            int yLocal,
            int zLocal,
            BitMask carveMask) {
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal - 1, yLocal, zLocal, carveMask))
            return true;
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal + 1, yLocal, zLocal, carveMask))
            return true;
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal, yLocal - 1, zLocal, carveMask))
            return true;
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal, yLocal + 1, zLocal, carveMask))
            return true;
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal, yLocal, zLocal - 1, carveMask))
            return true;
        return checkNeighbor(
                loaded, chunkX, chunkZ, subY, src, xLocal, yLocal, zLocal + 1, carveMask);
    }

    private static boolean checkNeighbor(
            @Nullable NonBlockingHashMapLong<LevelChunk> loaded,
            int chunkX,
            int chunkZ,
            int subY,
            LevelChunkSection src,
            int xLocal,
            int yLocal,
            int zLocal,
            BitMask carveMask) {
        if (xLocal >= 0
                && xLocal < 16
                && yLocal >= 0
                && yLocal < 16
                && zLocal >= 0
                && zLocal < 16) {
            int idx = Library.packLocal(xLocal, yLocal, zLocal);
            if (carveMask.get(idx)) return false;
            return !src.getBlockState(xLocal, yLocal, zLocal).isAir();
        }

        int nCx = chunkX, nCz = chunkZ, nSubY = subY;
        int nx = xLocal, ny = yLocal, nz = zLocal;
        if (xLocal < 0) {
            nCx--;
            nx = 15;
        } else if (xLocal >= 16) {
            nCx++;
            nx = 0;
        }
        if (zLocal < 0) {
            nCz--;
            nz = 15;
        } else if (zLocal >= 16) {
            nCz++;
            nz = 0;
        }
        if (yLocal < 0) {
            nSubY--;
            ny = 15;
        } else if (yLocal >= 16) {
            nSubY++;
            ny = 0;
        }

        if (loaded == null) return true;
        LevelChunk neighbor = loaded.get(ChunkPos.pack(nCx, nCz));
        if (neighbor == null) return true;
        LevelChunkSection[] nSections = neighbor.getSections();
        if (nSubY < 0 || nSubY >= nSections.length) return false;
        LevelChunkSection s = getSectionVolatile(nSections, nSubY);
        return s != null && !s.hasOnlyAir() && !s.getBlockState(nx, ny, nz).isAir();
    }

    @ThreadSafeMethod
    public static boolean casSectionAt(
            @Nullable LevelChunkSection expect,
            @Nullable LevelChunkSection update,
            LevelChunkSection @NotNull [] arr,
            int subY) {
        return U.compareAndSetReference(arr, UnsafeHolder.offReference(subY), expect, update);
    }

    @ThreadSafeMethod
    public static @Nullable LevelChunkSection getSectionVolatile(
            LevelChunkSection @NotNull [] arr, int subY) {
        return (LevelChunkSection) U.getReferenceVolatile(arr, UnsafeHolder.offReference(subY));
    }

    private static void onChunkLoad(ServerLevel server, LevelChunk chunk) {
        if (refCounter == 0) return;
        Identifier dim = server.dimension().identifier();
        if (activeTask.getInt(dim) == 0) return;
        NonBlockingHashMapLong<LevelChunk> mirror = chunkMap.get(dim);
        if (mirror != null) mirror.put(chunk.getPos().pack(), chunk);
    }

    private static void onChunkUnload(ServerLevel server, LevelChunk chunk) {
        if (refCounter == 0) return;
        NonBlockingHashMapLong<LevelChunk> mirror = chunkMap.get(server.dimension().identifier());
        if (mirror != null) mirror.remove(chunk.getPos().pack());
    }

    public static void onServerStopped() {
        chunkMap.clear();
        activeTask.clear();
        BLAST_LOAD_CLAIMS.clear();
        refCounter = 0;
    }

    public static int indexToX(int index, int chunkX) {
        return (chunkX << 4) | (index & 0xF);
    }

    public static int indexToY(int index) {
        return (index >>> 8) & 0xF;
    }

    public static int indexToZ(int index, int chunkZ) {
        return (chunkZ << 4) | ((index >>> 4) & 0xF);
    }

    @ThreadSafeMethod
    public static void applyToSnapshot(
            SectionSnapshot snapshot,
            Long2ObjectOpenHashMap<BlockState> newStates,
            Long2ObjectOpenHashMap<BlockState> oldStatesOut,
            Long2ObjectOpenHashMap<BlockState> deferredBeOut) {
        if (newStates.isEmpty()) return;
        LevelChunk chunk = snapshot.chunk();
        Level level = chunk.getLevel();
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        int sectionCount = snapshot.sections.length;
        Int2ObjectOpenHashMap<BlockState>[] bySub = TL_BUCKET.get();
        if (bySub.length < sectionCount) {
            @SuppressWarnings("unchecked")
            Int2ObjectOpenHashMap<BlockState>[] grown = new Int2ObjectOpenHashMap[sectionCount];
            System.arraycopy(bySub, 0, grown, 0, bySub.length);
            for (int i = bySub.length; i < sectionCount; i++)
                grown[i] = new Int2ObjectOpenHashMap<>();
            TL_BUCKET.set(grown);
            bySub = grown;
        }
        for (int i = 0; i < sectionCount; i++) bySub[i].clear();
        for (var iterator = newStates.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            long pos = entry.getLongKey();
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            assert (x >> 4) == chunkX && (z >> 4) == chunkZ;
            int index = level.getSectionIndex(y);
            assert index >= 0 && index < sectionCount;
            bySub[index].put(Library.packLocal(x & 15, y & 15, z & 15), entry.getValue());
        }
        for (int subIndex = 0; subIndex < sectionCount; subIndex++) {
            Int2ObjectOpenHashMap<BlockState> bucket = bySub[subIndex];
            if (bucket.isEmpty()) continue;
            LevelChunkSection section = snapshot.sections[subIndex];
            int yBase = (subIndex + level.getMinSectionY()) << 4;
            boolean changed = false;
            if (bucket.size() < DENSE_THRESHOLD) {
                for (var iterator = bucket.int2ObjectEntrySet().fastIterator();
                        iterator.hasNext(); ) {
                    var entry = iterator.next();
                    changed |=
                            applySnapshotCell(
                                    section,
                                    chunkX,
                                    chunkZ,
                                    yBase,
                                    entry.getIntKey(),
                                    entry.getValue(),
                                    oldStatesOut,
                                    deferredBeOut);
                }
            } else {
                BlockState[] overrides = TL_OVERRIDES.get();
                Arrays.fill(overrides, null);
                for (var iterator = bucket.int2ObjectEntrySet().fastIterator();
                        iterator.hasNext(); ) {
                    var entry = iterator.next();
                    overrides[entry.getIntKey()] = entry.getValue();
                }
                for (int index = 0; index < 4096; index++) {
                    BlockState replacement = overrides[index];
                    if (replacement != null) {
                        changed |=
                                applySnapshotCell(
                                        section,
                                        chunkX,
                                        chunkZ,
                                        yBase,
                                        index,
                                        replacement,
                                        oldStatesOut,
                                        deferredBeOut);
                    }
                }
            }
            if (changed) snapshot.changed(subIndex);
        }
    }

    private static boolean applySnapshotCell(
            LevelChunkSection section,
            int chunkX,
            int chunkZ,
            int yBase,
            int index,
            BlockState replacement,
            Long2ObjectOpenHashMap<BlockState> oldStatesOut,
            Long2ObjectOpenHashMap<BlockState> deferredBeOut) {
        int x = Library.getLocalX(index);
        int y = Library.getLocalY(index);
        int z = Library.getLocalZ(index);
        BlockState previous = section.getBlockState(x, y, z);
        if (previous == replacement) return false;
        long pos = BlockPos.asLong((chunkX << 4) | x, yBase | y, (chunkZ << 4) | z);
        if (previous.hasBlockEntity() || replacement.hasBlockEntity()) {
            deferredBeOut.put(pos, replacement);
            return false;
        }
        section.setBlockState(x, y, z, replacement, false);
        oldStatesOut.put(pos, previous);
        return true;
    }
}

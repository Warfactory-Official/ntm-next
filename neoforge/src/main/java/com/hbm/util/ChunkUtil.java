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

package com.hbm.util;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.handler.threading.BombForkJoinPool;
import com.hbm.interfaces.BitMask;
import com.hbm.interfaces.injected.BulkLightEngine;
import com.hbm.lib.maps.NonBlockingHashMapLong;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.OcclusionRefreshLayout;
import com.hbm.platform.ServerEvents;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.lighting.LightEngine;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public final class ChunkUtil {
    public static boolean resident(BlockEntity be, LevelChunk chunk) {
        return !be.isRemoved() && chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL);
    }

    public static void markUnsaved(BlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel server)) return;
        BlockPos pos = be.getBlockPos();
        LevelChunk chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null) chunk.markUnsaved();
    }

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
        final BiConsumer<ServerLevel, LevelChunk> callback = ChunkUtil::onChunkLoad;
        ServerEvents.CHUNK_LOAD.add(callback);
        final BiConsumer<ServerLevel, LevelChunk> callback2 = ChunkUtil::onChunkUnload;
        ServerEvents.CHUNK_UNLOAD.add(callback2);
        final Consumer<MinecraftServer> callback3 =
                server -> {
                    BombForkJoinPool.onServerStopped();
                    onServerStopped();
                };
        ServerEvents.STOPPING.add(callback3);
    }

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

    private static @Nullable LevelChunk fullChunkIfLoaded(@Nullable ChunkHolder holder) {
        if (holder == null || holder.getTicketLevel() > ChunkLevel.byStatus(FullChunkStatus.FULL))
            return null;
        return holder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).orElse(null);
    }

    public static @Nullable LevelChunk liveChunkNow(ServerLevel level, long chunkPos) {
        return fullChunkIfLoaded(
                level.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunkPos));
    }

    public static void forEachLoadedChunk(ServerLevel level, Consumer<LevelChunk> visitor) {
        for (ChunkHolder holder : level.getChunkSource().chunkMap.visibleChunkMap.values()) {
            LevelChunk chunk = fullChunkIfLoaded(holder);
            if (chunk != null) visitor.accept(chunk);
        }
    }

    public static void releaseMirrorMap(ServerLevel level) {
        Identifier dim = level.dimension().identifier();
        if (activeTask.addTo(dim, -1) == 1) {
            chunkMap.remove(dim);
            activeTask.removeInt(dim);
        }
        refCounter--;
    }

    public static void register() {
        BLAST_LOAD =
                Registrar.registerTicketType(
                        "blast_load",
                        () -> new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));
        ENTITY_HOLD =
                Registrar.registerTicketType(
                        "entity_hold",
                        () ->
                                new TicketType(
                                        40L,
                                        TicketType.FLAG_PERSIST
                                                | TicketType.FLAG_LOADING
                                                | TicketType.FLAG_SIMULATION
                                                | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    }

    public static void holdForEntity(ServerLevel level, ChunkPos chunkPos, int radius) {
        level.getChunkSource().addTicketWithRadius(ENTITY_HOLD.get(), chunkPos, radius);
    }

    public static void holdOwnChunk(Entity entity) {
        if (entity.level() instanceof ServerLevel server)
            holdForEntity(server, entity.chunkPosition(), HOLD_RADIUS);
    }

    public static void loadForEntity(ServerLevel level, ChunkPos chunkPos) {
        holdForEntity(level, chunkPos, HOLD_RADIUS);
        level.getChunk(chunkPos.x(), chunkPos.z());
    }

    public static void loadTickingForEntity(ServerLevel level, ChunkPos pos) {
        holdForEntity(level, pos, HOLD_RADIUS);
        flushChunkTickets(level);
        level.getChunk(pos.x(), pos.z());
        ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(pos.pack());
        CompletableFuture<ChunkResult<LevelChunk>> ticking = holder.getEntityTickingChunkFuture();
        level.getServer().managedBlock(ticking::isDone);
        ChunkResult<LevelChunk> result = ticking.join();
        result.orElseThrow(
                () ->
                        new IllegalStateException(
                                "entity-ticking chunk " + pos + ": " + result.getError()));
        level.waitForEntities(pos, 0);
    }

    public static void addBlastTicket(ServerLevel level, long chunkPos) {
        Long2IntOpenHashMap held =
                BLAST_LOAD_CLAIMS.computeIfAbsent(level, ignored -> new Long2IntOpenHashMap());
        if (held.addTo(chunkPos, 1) > 0) return;
        ChunkPos pos = new ChunkPos(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
        level.getChunkSource().addTicketWithRadius(BLAST_LOAD.get(), pos, 0);
    }

    public static void flushChunkTickets(ServerLevel level) {
        level.getChunkSource().runDistanceManagerUpdates();
    }

    public static @Nullable CompletableFuture<ChunkResult<LevelChunk>> blastChunkFuture(
            ServerLevel level, long chunkPos) {
        ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunkPos);
        return holder == null ? null : holder.getFullChunkFuture();
    }

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
        return chunkIfLoaded(
                level,
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static @Nullable LevelChunk chunkIfLoaded(Level level, int chunkX, int chunkZ) {
        return level instanceof ServerLevel server
                ? server.getChunkSource().getChunkNow(chunkX, chunkZ)
                : level.getChunkSource().getChunk(chunkX, chunkZ, false);
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

    public static @Nullable LevelChunk getLoadedChunk(
            NonBlockingHashMapLong<LevelChunk> mirror, long chunkPos) {
        return mirror.get(chunkPos);
    }

    public static LevelChunkSection @Nullable [] getLoadedSections(
            NonBlockingHashMapLong<LevelChunk> mirror, long chunkPos) {
        LevelChunk chunk = getLoadedChunk(mirror, chunkPos);
        return chunk == null ? null : chunk.getSections();
    }

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
        return LightEngine.hasDifferentLightProperties(
                (BlockGetter) chunk, pos, oldState, newState);
    }

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

    public static void dispatchRemovalHook(
            ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        Block newBlock = newState.getBlock();
        if (!oldState.is(newBlock) || newBlock instanceof BaseRailBlock) {
            oldState.affectNeighborsAfterRemoval(level, pos, false);
        }
    }

    public static CompletableFuture<?> updateLight(
            ServerLevel level, LevelChunk chunk, LongList positions, long sectionMaskFromBottom) {
        assert level.getServer().isSameThread();
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        if (lightEngine instanceof BulkLightEngine bulk) {
            return bulk.hbm$updateLight(chunk, positions, sectionMaskFromBottom);
        }
        return updateLightIndividually(level, chunk, positions, sectionMaskFromBottom);
    }

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

    public static CompletableFuture<Void> broadcastWholesaleResend(
            ServerLevel level, long chunkPos, CompletableFuture<?> lighting) {
        int cx = ChunkPos.getX(chunkPos);
        int cz = ChunkPos.getZ(chunkPos);
        MinecraftServer server = level.getServer();
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        return lighting.thenAcceptAsync(
                _ -> {
                    LevelChunk fresh = level.getChunkSource().getChunkNow(cx, cz);
                    if (fresh == null) return;
                    fresh.markUnsaved();
                    List<ServerPlayer> players =
                            level.getChunkSource().chunkMap.getPlayers(fresh.getPos(), false);
                    if (players.isEmpty()) return;
                    ClientboundLevelChunkWithLightPacket chunkPacket =
                            new ClientboundLevelChunkWithLightPacket(
                                    fresh, lightEngine, null, null);
                    for (ServerPlayer p : players) {
                        p.connection.send(chunkPacket);
                    }
                    PacketWire.sendToPlayers(OcclusionRefreshLayout.encode(chunkPos), players);
                },
                server);
    }

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
        LevelChunkSection src = SectionAccess.getVolatile(sections, subY);
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
            int xLocal = LocalBlockIndex.x(idx);
            int yLocal = LocalBlockIndex.y(idx);
            int zLocal = LocalBlockIndex.z(idx);

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
                    LocalBlockIndex.x(idx),
                    LocalBlockIndex.y(idx),
                    LocalBlockIndex.z(idx),
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
            int idx = LocalBlockIndex.pack(xLocal, yLocal, zLocal);
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
        LevelChunkSection s = SectionAccess.getVolatile(nSections, nSubY);
        return s != null && !s.hasOnlyAir() && !s.getBlockState(nx, ny, nz).isAir();
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

    public static void applyToSnapshot(
            SectionSnapshot snapshot,
            Long2ObjectOpenHashMap<BlockState> newStates,
            Long2ObjectOpenHashMap<BlockState> oldStatesOut,
            Long2ObjectOpenHashMap<BlockState> deferredBeOut) {
        LevelChunk chunk = snapshot.chunk();
        applyToSections(
                snapshot.sections,
                chunk.getMinSectionY(),
                chunk.getPos().x(),
                chunk.getPos().z(),
                newStates,
                oldStatesOut,
                deferredBeOut,
                false,
                snapshot::changed);
    }

    public static void applyToSections(
            LevelChunkSection[] sections,
            int minSectionY,
            int chunkX,
            int chunkZ,
            Long2ObjectOpenHashMap<BlockState> newStates,
            Long2ObjectOpenHashMap<BlockState> oldStatesOut,
            Long2ObjectOpenHashMap<BlockState> deferredOut,
            boolean deferSurfaces,
            IntConsumer changedSection) {
        if (newStates.isEmpty()) return;
        int sectionCount = sections.length;
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
            int index = (y >> 4) - minSectionY;
            assert index >= 0 && index < sectionCount;
            bySub[index].put(LocalBlockIndex.pack(x & 15, y & 15, z & 15), entry.getValue());
        }
        for (int subIndex = 0; subIndex < sectionCount; subIndex++) {
            Int2ObjectOpenHashMap<BlockState> bucket = bySub[subIndex];
            if (bucket.isEmpty()) continue;
            LevelChunkSection section = sections[subIndex];
            int yBase = (subIndex + minSectionY) << 4;
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
                                    deferredOut,
                                    deferSurfaces);
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
                                        deferredOut,
                                        deferSurfaces);
                    }
                }
            }
            if (changed) changedSection.accept(subIndex);
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
            Long2ObjectOpenHashMap<BlockState> deferredOut,
            boolean deferSurfaces) {
        int x = LocalBlockIndex.x(index);
        int y = LocalBlockIndex.y(index);
        int z = LocalBlockIndex.z(index);
        BlockState previous = section.getBlockState(x, y, z);
        long pos = BlockPos.asLong((chunkX << 4) | x, yBase | y, (chunkZ << 4) | z);
        if (previous == replacement) {
            deferredOut.remove(pos);
            return false;
        }
        if (previous.hasBlockEntity()
                || replacement.hasBlockEntity()
                || deferSurfaces
                        && (MultiblockSurface.isSurface(previous)
                                || MultiblockSurface.isSurface(replacement))) {
            deferredOut.put(pos, replacement);
            return false;
        }
        section.setBlockState(x, y, z, replacement, false);
        oldStatesOut.putIfAbsent(pos, previous);
        deferredOut.remove(pos);
        return true;
    }
}

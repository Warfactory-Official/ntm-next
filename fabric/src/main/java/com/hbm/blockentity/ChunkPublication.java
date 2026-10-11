// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.handler.UnstableFuses;
import com.hbm.platform.FabricServerEvents;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public final class ChunkPublication {

    private static final List<Loading> LOADING = new ArrayList<>(2);
    private static final List<LevelChunk> RISEN = new ArrayList<>();

    private ChunkPublication() {}

    public static void init() {
        FabricServerEvents.onServerStopping(
                server -> {
                    LOADING.clear();
                    RISEN.clear();
                });
    }

    private static final class Loading {
        final ServerLevel level;
        final LevelChunk chunk;
        boolean registering = true;

        Loading(ServerLevel level, LevelChunk chunk) {
            this.level = level;
            this.chunk = chunk;
        }
    }

    public static void enter(ServerLevel level, LevelChunk chunk) {
        LOADING.add(new Loading(level, chunk));
        level.getChunkSource().clearCache();
    }

    public static void abandon(LevelChunk chunk) {
        Loading entry = entry(chunk);
        if (entry != null) LOADING.remove(entry);
    }

    public static void registered(ServerLevel level, LevelChunk chunk) {
        Loading entry = entry(chunk);
        if (entry == null) return;
        entry.registering = false;
        try {
            for (BlockEntity be : List.copyOf(chunk.getBlockEntities().values())) {
                if (!be.isRemoved()) graphLoad(level, be);
            }
            level.hbm$publishedChunks().add(chunk.getPos().pack());
            promoteNow(level, chunk);
        } finally {
            LOADING.remove(entry);
        }
    }

    public static @Nullable LevelChunk loading(ServerLevel level, int x, int z) {
        for (int i = LOADING.size() - 1; i >= 0; i--) {
            Loading entry = LOADING.get(i);
            ChunkPos pos = entry.chunk.getPos();
            if (entry.level == level && pos.x() == x && pos.z() == z) return entry.chunk;
        }
        return null;
    }

    public static @Nullable LevelChunk loading(ChunkHolder holder) {
        if (LOADING.isEmpty()) return null;
        ChunkAccess latest = holder.getLatestChunk();
        LevelChunk chunk =
                latest instanceof ImposterProtoChunk imposter
                        ? imposter.getWrapped()
                        : latest instanceof LevelChunk full ? full : null;
        return chunk != null && entry(chunk) != null ? chunk : null;
    }

    public static boolean publishing(ServerLevel level, BlockPos pos) {
        Loading entry = entry(level, pos);
        return entry != null && !entry.registering;
    }

    public static void added(ServerLevel level, BlockEntity be) {
        UnstableFuses.onBlockLoad(level, be);
        if (be instanceof GraphResident) GraphResident.attach(level, be);
        if (be instanceof IFluidHandlerMK2 handler) {
            handler.forEachTank(tank -> tank.bindOwner(be));
            FluidPipeBlock.scheduleConnections(be);
        }
        Loading entry = entry(level, be.getBlockPos());
        if (entry == null || !entry.registering) graphLoad(level, be);
    }

    private static void graphLoad(ServerLevel level, BlockEntity be) {
        if (be instanceof GraphResident resident) resident.onGraphLoad(level);
        be.getBlockState().getBlock().hbm$blockEntityLoaded(level, be);
        if (be instanceof GraphResident) PhysicalGraph.refreshAround(level, be.getBlockPos());
    }

    public static void risen(LevelChunk chunk) {
        RISEN.add(chunk);
    }

    public static void settle(ServerLevel level) {
        if (RISEN.isEmpty()) return;
        List<LevelChunk> risen = null;
        for (int i = RISEN.size() - 1; i >= 0; i--) {
            if (RISEN.get(i).getLevel() != level) continue;
            if (risen == null) risen = new ArrayList<>();
            risen.add(RISEN.remove(i));
        }
        if (risen == null) return;
        level.getChunkSource().clearCache();
        for (int i = risen.size() - 1; i >= 0; i--) {
            LevelChunk chunk = risen.get(i);
            if (chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) promote(level, chunk);
        }
    }

    public static void promote(ServerLevel level, LevelChunk chunk) {
        if (level.hbm$publishedChunks().add(chunk.getPos().pack())) promoteNow(level, chunk);
    }

    public static void demote(ServerLevel level, ChunkPos pos) {
        level.hbm$publishedChunks().remove(pos.pack());
    }

    private static void promoteNow(ServerLevel level, LevelChunk chunk) {
        LongOpenHashSet members = AssembledMembers.declareIn(level, chunk);
        LevelNodeGraph.onChunkFull(level, chunk);
        FlushIndex.onChunkFull(level, chunk);
        if (members != null) AssembledMembers.check(level, members);
    }

    private static @Nullable Loading entry(LevelChunk chunk) {
        for (int i = LOADING.size() - 1; i >= 0; i--) {
            if (LOADING.get(i).chunk == chunk) return LOADING.get(i);
        }
        return null;
    }

    private static @Nullable Loading entry(ServerLevel level, BlockPos pos) {
        if (LOADING.isEmpty()) return null;
        int x = SectionPos.blockToSectionCoord(pos.getX());
        int z = SectionPos.blockToSectionCoord(pos.getZ());
        for (int i = LOADING.size() - 1; i >= 0; i--) {
            Loading entry = LOADING.get(i);
            ChunkPos at = entry.chunk.getPos();
            if (entry.level == level && at.x() == x && at.z() == z) return entry;
        }
        return null;
    }

    public static SignalGetter signals(ServerLevel level, BlockPos origin) {
        ChunkHolder holder =
                level.getChunkSource().chunkMap.getUpdatingChunkIfPresent(ChunkPos.pack(origin));
        return new Signals(level, holder.getLatestChunk());
    }

    private record Signals(ServerLevel level, ChunkAccess owner) implements SignalGetter {
        private @Nullable ChunkAccess chunk(BlockPos pos) {
            int x = pos.getX() >> 4, z = pos.getZ() >> 4;
            if (owner.getPos().x() == x && owner.getPos().z() == z) return owner;
            ChunkHolder holder =
                    level.getChunkSource().chunkMap.getUpdatingChunkIfPresent(ChunkPos.pack(x, z));
            return holder == null ? null : holder.getChunkIfPresentUnchecked(ChunkStatus.FEATURES);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            ChunkAccess chunk = chunk(pos);
            return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos);
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            ChunkAccess chunk = chunk(pos);
            return chunk == null ? null : chunk.getBlockEntity(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinY() {
            return level.getMinY();
        }
    }
}

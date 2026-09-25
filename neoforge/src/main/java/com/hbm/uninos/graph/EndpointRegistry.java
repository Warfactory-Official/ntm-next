// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.platform.Services;
import com.hbm.util.ChunkPositionIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class EndpointRegistry {

    private static final int NET_DOMAINS =
            MachineCaps.POWER_IN
                    | MachineCaps.POWER_OUT
                    | MachineCaps.FLUID_IN
                    | MachineCaps.FLUID_OUT
                    | MachineCaps.FE;
    private final Long2ObjectOpenHashMap<Endpoint> byCell = new Long2ObjectOpenHashMap<>();
    private final ChunkPositionIndex byChunk = new ChunkPositionIndex();

    public static EndpointRegistry of(ServerLevel level) {
        return level.hbm$endpoints();
    }

    public static void init() {
        Services.SERVER.onChunkUnload(
                (level, chunk) -> of(level).onChunkUnload(chunk.getPos().pack()));
    }

    public void onChunkUnload(long chunkKey) {
        LongOpenHashSet cells = byChunk.take(chunkKey);
        if (cells == null) return;
        for (LongIterator it = cells.iterator(); it.hasNext(); ) byCell.remove(it.nextLong());
    }

    public static void declareSelfIfEndpoint(ServerLevel level, BlockEntity be) {
        int domains = selfDeclaredBits(be);
        if ((domains & NET_DOMAINS) != 0) {
            level.hbm$endpoints()
                    .declare(
                            be.getBlockPos(),
                            be.getBlockPos(),
                            BlockMultiblockCore.MASK_ALL,
                            domains);
        }
    }

    public static boolean isSelfEndpoint(BlockEntity be) {
        return (selfDeclaredBits(be) & NET_DOMAINS) != 0;
    }

    private static int selfDeclaredBits(BlockEntity be) {
        return be.getBlockState().getBlock() instanceof ICapabilityBlock declaring
                ? declaring.caps().declaredBits()
                : 0;
    }

    private static boolean stillFull(@Nullable WeakReference<LevelChunk> memo) {
        LevelChunk chunk = memo == null ? null : memo.get();
        return chunk != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL);
    }

    public void declare(BlockPos cell, BlockPos owner, int faceMask, int domains) {
        long cellKey = cell.asLong();
        if (faceMask == 0) {
            byCell.remove(cellKey);
            byChunk.remove(cellKey);
            return;
        }
        byCell.put(cellKey, new Endpoint(owner.asLong(), faceMask, domains));
        byChunk.add(cellKey);
    }

    public void withdraw(BlockPos cell) {
        byCell.remove(cell.asLong());
        byChunk.remove(cell.asLong());
    }

    public @Nullable Endpoint declared(long cellKey) {
        return byCell.get(cellKey);
    }

    public @Nullable BlockEntity resolve(
            ServerLevel level, long cellKey, @Nullable Direction face, int domains) {
        Endpoint ep = byCell.get(cellKey);
        if (ep == null) return null;
        if ((ep.domains() & domains) == 0) return null;
        if (face != null && !ep.isOpen(face)) return null;
        WeakReference<BlockEntity> memo = ep.resolved;
        BlockEntity cached = memo == null ? null : memo.get();
        if (cached != null && !cached.isRemoved() && stillFull(ep.resolvedChunk)) return cached;
        BlockPos owner = BlockPos.of(ep.owner());
        LevelChunk chunk = BlockMultiblockCore.readableChunk(level, owner.getX(), owner.getZ());
        if (chunk == null) {
            ep.resolved = null;
            ep.resolvedChunk = null;
            return null;
        }
        BlockEntity be = chunk.getBlockEntity(owner);
        if (be == null || be.isRemoved()) {
            ep.resolved = null;
            ep.resolvedChunk = null;
            return null;
        }
        ep.resolved = new WeakReference<>(be);
        ep.resolvedChunk = new WeakReference<>(chunk);
        return be;
    }

    public int size() {
        return byCell.size();
    }

    public static final class Endpoint {

        private final long owner;
        private final int faceMask;
        private final int domains;

        private @Nullable WeakReference<BlockEntity> resolved;

        private @Nullable WeakReference<LevelChunk> resolvedChunk;

        public Endpoint(long owner, int faceMask, int domains) {
            this.owner = owner;
            this.faceMask = faceMask;
            this.domains = domains;
        }

        public long owner() {
            return owner;
        }

        public int faceMask() {
            return faceMask;
        }

        public int domains() {
            return domains;
        }

        public boolean isOpen(Direction face) {
            return (faceMask & (1 << face.ordinal())) != 0;
        }
    }
}

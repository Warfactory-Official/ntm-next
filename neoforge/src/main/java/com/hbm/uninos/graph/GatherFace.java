// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.platform.BlockLookupCache;
import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public abstract class GatherFace<H, V, E> {

    public final long posKey;
    public final BlockPos npos;
    public final @Nullable Direction side;
    public final BlockLookupCache<H> provider;
    public final BlockLookupCache<H> receiver;
    public @Nullable H memoProvider;
    public @Nullable H memoReceiver;
    private @Nullable BlockEntity anchor;
    private @Nullable LevelChunk anchorChunk;
    private @Nullable BlockLookupCache<V> foreign;
    private @Nullable E endpoint;

    protected GatherFace(
            long posKey,
            @Nullable Direction side,
            BlockLookupCache<H> provider,
            BlockLookupCache<H> receiver) {
        this.posKey = posKey;
        this.side = side;
        this.npos = BlockPos.of(posKey);
        this.provider = provider;
        this.receiver = receiver;
    }

    protected abstract BlockLookupCache<V> foreignCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side);

    protected abstract E newEndpoint(V store);

    protected abstract void rebind(E endpoint, V store);

    public final @Nullable E foreign(ServerLevel level) {
        if (foreign == null) foreign = foreignCache(level, npos, side);
        V store = foreign.find();
        if (store == null) return null;
        if (endpoint == null) endpoint = newEndpoint(store);
        else rebind(endpoint, store);
        return endpoint;
    }

    public boolean memoLive() {
        BlockEntity be = anchor;
        if (be == null) return false;
        if (ChunkUtil.resident(be, anchorChunk)) return true;
        disarm();
        return false;
    }

    public void disarm() {
        anchor = null;
        anchorChunk = null;
        memoProvider = null;
        memoReceiver = null;
    }

    public void memoise(ServerLevel level, @Nullable BlockEntity owner) {
        if (owner == null) return;
        H p = provider.find();
        H r = receiver.find();
        BlockEntity pa = p == null ? null : p instanceof BlockEntity be ? be : owner;
        BlockEntity ra = r == null ? null : r instanceof BlockEntity be ? be : owner;
        BlockEntity be = pa != null ? pa : ra;
        if (be == null || (pa != null && ra != null && pa != ra) || be.isRemoved()) return;
        LevelChunk chunk =
                BlockMultiblockCore.readableChunk(
                        level, be.getBlockPos().getX(), be.getBlockPos().getZ());
        if (chunk == null) return;
        anchor = be;
        anchorChunk = chunk;
        memoProvider = p;
        memoReceiver = r;
    }
}

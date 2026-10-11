// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.util.ChunkUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public abstract class ResidentLookup<T> implements BlockLookupCache<T> {

    protected final ServerLevel level;
    private @Nullable T memo;
    private @Nullable LevelChunk memoChunk;

    protected ResidentLookup(ServerLevel level) {
        this.level = level;
    }

    protected abstract @Nullable T lookup();

    protected abstract @Nullable T fresh(T found);

    @Override
    public final @Nullable T find() {
        T cached = memo;
        if (cached != null && ChunkUtil.resident((BlockEntity) cached, memoChunk)) return cached;
        invalidate();
        T found = lookup();
        if (found == null) return null;
        if (!(found instanceof BlockEntity be)) return fresh(found);
        LevelChunk chunk =
                BlockMultiblockCore.readableChunk(
                        level, be.getBlockPos().getX(), be.getBlockPos().getZ());
        if (chunk == null || be.isRemoved()) return null;
        memo = found;
        memoChunk = chunk;
        return found;
    }

    public final void invalidate() {
        memo = null;
        memoChunk = null;
    }
}

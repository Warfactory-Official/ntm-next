// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public final class ChunkPositionIndex {

    private final Long2ObjectOpenHashMap<LongOpenHashSet> byChunk = new Long2ObjectOpenHashMap<>();

    public static long chunkOf(long posKey) {
        return ChunkPos.pack(
                SectionPos.blockToSectionCoord(BlockPos.getX(posKey)),
                SectionPos.blockToSectionCoord(BlockPos.getZ(posKey)));
    }

    public void add(long posKey) {
        byChunk.computeIfAbsent(chunkOf(posKey), k -> new LongOpenHashSet()).add(posKey);
    }

    public void remove(long posKey) {
        long chunkKey = chunkOf(posKey);
        LongOpenHashSet inChunk = byChunk.get(chunkKey);
        if (inChunk == null) return;
        inChunk.remove(posKey);
        if (inChunk.isEmpty()) byChunk.remove(chunkKey);
    }

    public @Nullable LongOpenHashSet take(long chunkKey) {
        return byChunk.remove(chunkKey);
    }

    public int chunkCount() {
        return byChunk.size();
    }

    public int positionCount(long chunkKey) {
        LongOpenHashSet inChunk = byChunk.get(chunkKey);
        return inChunk == null ? 0 : inChunk.size();
    }
}

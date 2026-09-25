// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.platform;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class CapCacheIndex {

    private final Long2ObjectOpenHashMap<Long2ObjectOpenHashMap<List<WeakReference<Invalidatable>>>>
            byChunk = new Long2ObjectOpenHashMap<>();

    private static void sweep(List<WeakReference<Invalidatable>> bucket) {
        for (int i = bucket.size() - 1; i >= 0; i--) {
            Invalidatable cache = bucket.get(i).get();
            if (cache == null) bucket.remove(i);
            else cache.hbm$invalidate();
        }
    }

    private static long chunkOf(long posKey) {
        return ChunkPos.pack(
                SectionPos.blockToSectionCoord(BlockPos.getX(posKey)),
                SectionPos.blockToSectionCoord(BlockPos.getZ(posKey)));
    }

    public void register(BlockPos pos, Invalidatable cache) {
        byChunk.computeIfAbsent(ChunkPos.pack(pos), k -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(pos.asLong(), k -> new ArrayList<>())
                .add(new WeakReference<>(cache));
    }

    public void invalidate(long posKey) {
        long chunkKey = chunkOf(posKey);
        Long2ObjectOpenHashMap<List<WeakReference<Invalidatable>>> inChunk = byChunk.get(chunkKey);
        if (inChunk == null) return;
        List<WeakReference<Invalidatable>> bucket = inChunk.get(posKey);
        if (bucket == null) return;
        sweep(bucket);
        if (bucket.isEmpty()) inChunk.remove(posKey);
        if (inChunk.isEmpty()) byChunk.remove(chunkKey);
    }

    public void invalidateChunk(long chunkKey) {
        Long2ObjectOpenHashMap<List<WeakReference<Invalidatable>>> inChunk = byChunk.get(chunkKey);
        if (inChunk == null) return;
        for (var it = inChunk.long2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
            var e = it.next();
            sweep(e.getValue());
            if (e.getValue().isEmpty()) it.remove();
        }
        if (inChunk.isEmpty()) byChunk.remove(chunkKey);
    }

    public interface Invalidatable {
        void hbm$invalidate();
    }
}

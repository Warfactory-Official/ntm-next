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

package com.hbm.fabric.platform;

import com.hbm.platform.ResidentLookup;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class CapCacheIndex {

    private final Long2ObjectOpenHashMap<
                    Long2ObjectOpenHashMap<List<WeakReference<ResidentLookup<?>>>>>
            byChunk = new Long2ObjectOpenHashMap<>();

    private static void sweep(List<WeakReference<ResidentLookup<?>>> bucket) {
        for (int i = bucket.size() - 1; i >= 0; i--) {
            ResidentLookup<?> cache = bucket.get(i).get();
            if (cache == null) bucket.remove(i);
            else cache.invalidate();
        }
    }

    private static long chunkOf(long posKey) {
        return ChunkPos.pack(
                SectionPos.blockToSectionCoord(BlockPos.getX(posKey)),
                SectionPos.blockToSectionCoord(BlockPos.getZ(posKey)));
    }

    public void register(BlockPos pos, ResidentLookup<?> cache) {
        byChunk.computeIfAbsent(ChunkPos.pack(pos), k -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(pos.asLong(), k -> new ArrayList<>())
                .add(new WeakReference<>(cache));
    }

    public void invalidate(long posKey) {
        long chunkKey = chunkOf(posKey);
        Long2ObjectOpenHashMap<List<WeakReference<ResidentLookup<?>>>> inChunk =
                byChunk.get(chunkKey);
        if (inChunk == null) return;
        List<WeakReference<ResidentLookup<?>>> bucket = inChunk.get(posKey);
        if (bucket == null) return;
        sweep(bucket);
        if (bucket.isEmpty()) inChunk.remove(posKey);
        if (inChunk.isEmpty()) byChunk.remove(chunkKey);
    }

    public void invalidateChunk(long chunkKey) {
        Long2ObjectOpenHashMap<List<WeakReference<ResidentLookup<?>>>> inChunk =
                byChunk.get(chunkKey);
        if (inChunk == null) return;
        for (var it = inChunk.long2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
            var e = it.next();
            sweep(e.getValue());
            if (e.getValue().isEmpty()) it.remove();
        }
        if (inChunk.isEmpty()) byChunk.remove(chunkKey);
    }
}

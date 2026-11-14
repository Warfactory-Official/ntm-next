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

    public @Nullable LongOpenHashSet get(long chunkKey) {
        return byChunk.get(chunkKey);
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

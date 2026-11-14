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

package com.hbm.explosion;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class FalloutReplay {
    final Long2ObjectOpenHashMap<BlockState> changed = new Long2ObjectOpenHashMap<>();
    final Long2ObjectOpenHashMap<BlockState> deferred = new Long2ObjectOpenHashMap<>();
    final LongOpenHashSet cores = new LongOpenHashSet();
    private final int minY;
    LongArrayList parsedCells;
    List<BlockState> parsedOld;
    long[] lightChecks;

    public FalloutReplay(int minY) {
        this.minY = minY;
    }

    public FalloutReplay copy() {
        var copy = new FalloutReplay(minY);
        copy.changed.putAll(changed);
        copy.deferred.putAll(deferred);
        copy.cores.addAll(cores);
        return copy;
    }

    public boolean persistable() {
        return !changed.isEmpty() || !deferred.isEmpty() || !cores.isEmpty();
    }

    public CompoundTag write() {
        var palette = new ObjectArrayList<BlockState>();
        var indices = new Object2IntOpenHashMap<BlockState>();
        indices.defaultReturnValue(-1);
        var tag = new CompoundTag();
        tag.putIntArray("changed", pack(changed, minY, palette, indices));
        tag.putIntArray("deferred", pack(deferred, minY, palette, indices));
        tag.putLongArray("cores", cores.toLongArray());
        var states = new ListTag();
        for (BlockState state : palette) states.add(NbtUtils.writeBlockState(state));
        tag.put("palette", states);
        return tag;
    }

    public static FalloutReplay read(
            CompoundTag tag, int chunkX, int chunkZ, int minY, HolderGetter<Block> blocks) {
        var replay = new FalloutReplay(minY);
        var palette = new ArrayList<BlockState>();
        for (Tag state : tag.getListOrEmpty("palette"))
            palette.add(NbtUtils.readBlockState(blocks, (CompoundTag) state));
        unpack(
                tag.getIntArray("changed").orElseThrow(),
                replay.changed,
                chunkX,
                chunkZ,
                minY,
                palette);
        unpack(
                tag.getIntArray("deferred").orElseThrow(),
                replay.deferred,
                chunkX,
                chunkZ,
                minY,
                palette);
        for (long core : tag.getLongArray("cores").orElseThrow()) replay.cores.add(core);
        return replay;
    }

    private static int[] pack(
            Long2ObjectOpenHashMap<BlockState> cells,
            int minY,
            List<BlockState> palette,
            Object2IntOpenHashMap<BlockState> indices) {
        int[] packed = new int[cells.size()];
        int i = 0;
        for (var iterator = cells.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            int index = indices.getInt(entry.getValue());
            if (index < 0) {
                index = palette.size();
                if (index >= 1 << 12)
                    throw new IllegalStateException("Fallout replay palette exceeds 4096 states");
                palette.add(entry.getValue());
                indices.put(entry.getValue(), index);
            }
            long pos = entry.getLongKey();
            packed[i++] =
                    index << 20
                            | (BlockPos.getY(pos) - minY) << 8
                            | (BlockPos.getZ(pos) & 15) << 4
                            | BlockPos.getX(pos) & 15;
        }
        return packed;
    }

    private static void unpack(
            int[] packed,
            Long2ObjectOpenHashMap<BlockState> cells,
            int chunkX,
            int chunkZ,
            int minY,
            List<BlockState> palette) {
        for (int value : packed) {
            cells.put(
                    BlockPos.asLong(
                            chunkX << 4 | value & 15,
                            minY + (value >>> 8 & 0xFFF),
                            chunkZ << 4 | value >>> 4 & 15),
                    palette.get(value >>> 20));
        }
    }
}

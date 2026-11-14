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

package com.hbm.world.bulk;

import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

public final class BulkSession {
    private static final long[] EMPTY = new long[0];
    private static long nextClientId;
    private final IdentityHashMap<ServerLevel, Claims> levels = new IdentityHashMap<>();

    public static long nextClientId() {
        return ++nextClientId;
    }

    public void recordWrite(ServerLevel level, long pos) {
        levels.computeIfAbsent(level, ignored -> new Claims()).written.add(pos);
    }

    public boolean wasWritten(ServerLevel level, long pos) {
        Claims claims = levels.get(level);
        return claims != null && claims.written.contains(pos);
    }

    public void register(ServerLevel level, BlockPos owner, long[] cells) {
        levels.computeIfAbsent(level, ignored -> new Claims())
                .ownerIds
                .put(
                        owner.asLong(),
                        BuiltInRegistries.BLOCK.getId(level.getBlockState(owner).getBlock()));
        for (long cell : cells) {
            BlockPos target =
                    owner.offset(
                            MultiblockCoreIndex.dxOf(cell),
                            MultiblockCoreIndex.dyOf(cell),
                            MultiblockCoreIndex.dzOf(cell));
            if (!level.isOutsideBuildHeight(target)
                    && level.getWorldBorder().isWithinBounds(target))
                claim(level, owner, target, MultiblockCoreIndex.populationOf(cell));
        }
    }

    public void claim(ServerLevel level, BlockPos owner, BlockPos cell, int population) {
        if (population < MultiblockCoreIndex.FOLDED || population > MultiblockCoreIndex.MEMBER)
            throw new IllegalArgumentException("Unknown bulk ownership population " + population);
        Claims claims = levels.computeIfAbsent(level, ignored -> new Claims());
        long packed =
                MultiblockCoreIndex.pack(
                        0,
                        owner.getX() - cell.getX(),
                        owner.getY() - cell.getY(),
                        owner.getZ() - cell.getZ(),
                        population);
        if (claims.cells.put(cell.asLong(), packed) == packed) return;
        claims.ownerIds.put(
                owner.asLong(),
                BuiltInRegistries.BLOCK.getId(level.getBlockState(owner).getBlock()));
        claims.owned
                .computeIfAbsent(owner.asLong(), ignored -> new LongArrayList())
                .add(
                        MultiblockCoreIndex.pack(
                                0,
                                cell.getX() - owner.getX(),
                                cell.getY() - owner.getY(),
                                cell.getZ() - owner.getZ(),
                                population));
    }

    public long at(ServerLevel level, long pos) {
        Claims claims = levels.get(level);
        return claims == null ? MultiblockCoreIndex.NO_ENTRY : claims.cells.get(pos);
    }

    public int ownerId(ServerLevel level, long owner) {
        return levels.get(level).ownerIds.get(owner);
    }

    public long[] cells(ServerLevel level, BlockPos owner) {
        Claims claims = levels.get(level);
        LongArrayList candidates = claims == null ? null : claims.owned.get(owner.asLong());
        if (candidates == null) return EMPTY;
        LongArrayList result = new LongArrayList(candidates.size());
        for (long candidate : candidates) {
            BlockPos cell =
                    owner.offset(
                            MultiblockCoreIndex.dxOf(candidate),
                            MultiblockCoreIndex.dyOf(candidate),
                            MultiblockCoreIndex.dzOf(candidate));
            long current = claims.cells.get(cell.asLong());
            long expected =
                    MultiblockCoreIndex.pack(
                            0,
                            owner.getX() - cell.getX(),
                            owner.getY() - cell.getY(),
                            owner.getZ() - cell.getZ(),
                            MultiblockCoreIndex.populationOf(candidate));
            if (current == expected) {
                result.add(candidate);
            }
        }
        return result.toLongArray();
    }

    public void clear() {
        levels.clear();
    }

    private static final class Claims {
        final LongOpenHashSet written = new LongOpenHashSet();
        final Long2LongOpenHashMap cells = new Long2LongOpenHashMap();
        final Long2IntOpenHashMap ownerIds = new Long2IntOpenHashMap();
        final Long2ObjectOpenHashMap<LongArrayList> owned = new Long2ObjectOpenHashMap<>();

        private Claims() {
            cells.defaultReturnValue(MultiblockCoreIndex.NO_ENTRY);
            ownerIds.defaultReturnValue(-1);
        }
    }
}

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

package com.hbm.entity.item;

import com.hbm.interfaces.injected.ChunkExtension;
import com.hbm.packet.ChunkTrackerIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class PendingFallers {
    private PendingFallers() {}

    public static boolean canDefer(ServerLevel level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ChunkHolder holder =
                level.getChunkSource()
                        .chunkMap
                        .getVisibleChunkIfPresent(ChunkPos.pack(chunkX, chunkZ));
        return holder != null
                && !holder.getFullStatus().isOrAfter(FullChunkStatus.ENTITY_TICKING)
                && !level.isPositionEntityTicking(pos)
                && !ChunkTrackerIndex.isTracked(level, chunkX, chunkZ);
    }

    public static void defer(LevelChunk chunk, BlockPos pos, BlockState state) {
        assert chunk.getPos().x() == (pos.getX() >> 4) && chunk.getPos().z() == (pos.getZ() >> 4);
        append(chunk, pos.asLong(), state);
        chunk.markUnsaved();
    }

    public static void appendAll(ChunkExtension chunk, Long2ObjectMap<BlockState> fallers) {
        if (fallers.isEmpty()) return;
        var present = new LongOpenHashSet();
        long[] positions = chunk.hbm$pendingFallerPositions();
        for (int i = 0, size = chunk.hbm$pendingFallerSize(); i < size; i++)
            present.add(positions[i]);
        for (var entry : Long2ObjectMaps.fastIterable(fallers)) {
            if (present.add(entry.getLongKey()))
                append(chunk, entry.getLongKey(), entry.getValue());
        }
    }

    private static void append(ChunkExtension chunk, long pos, BlockState state) {
        int size = chunk.hbm$pendingFallerSize();
        long[] positions = chunk.hbm$pendingFallerPositions();
        BlockState[] states = chunk.hbm$pendingFallerStates();
        if (positions == null) {
            positions = new long[16];
            states = new BlockState[16];
        } else if (size == positions.length) {
            positions = Arrays.copyOf(positions, size << 1);
            states = Arrays.copyOf(states, size << 1);
        }
        positions[size] = pos;
        states[size] = state;
        chunk.hbm$setPendingFallers(positions, states, size + 1);
    }

    public static void promote(ServerLevel level, LevelChunk chunk) {
        int size = ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$pendingFallerSize();
        if (size == 0) return;
        long[] positions =
                ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$pendingFallerPositions();
        BlockState[] states =
                ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$pendingFallerStates();
        FallingBlockBatchEntity batch = null;
        for (int i = 0; i < size; i++) {
            if (FallingBlockBatchEntity.eligible(states[i])) {
                if (batch != null && batch.fallerCount() == FallingBlockBatchEntity.MAX_CHILDREN) {
                    if (!level.addFreshEntity(batch)) {
                        throw new IllegalStateException(
                                "Could not promote fallout faller batch at " + chunk.getPos());
                    }
                    batch = null;
                }
                if (batch == null) batch = FallingBlockBatchEntity.create(level, chunk);
                batch.add(BlockPos.of(positions[i]), states[i]);
            } else {
                FallingBlockNTEntity falling =
                        FallingBlockNTEntity.fall(level, BlockPos.of(positions[i]), states[i]);
                falling.dropItem = false;
            }
        }
        if (batch != null && !level.addFreshEntity(batch)) {
            throw new IllegalStateException(
                    "Could not promote fallout faller batch at " + chunk.getPos());
        }
        ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$setPendingFallers(null, null, 0);
        chunk.markUnsaved();
    }

    public static ListTag save(long[] positions, BlockState[] states, int size) {
        ListTag list = new ListTag();
        for (int i = 0; i < size; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", positions[i]);
            entry.put("state", NbtUtils.writeBlockState(states[i]));
            list.add(entry);
        }
        return list;
    }

    public static void load(ListTag list, ChunkExtension chunk) {
        int size = list.size();
        if (size == 0) return;
        long[] positions = new long[size];
        BlockState[] states = new BlockState[size];
        for (int i = 0; i < size; i++) {
            CompoundTag entry = list.getCompound(i).orElseThrow();
            positions[i] = entry.getLong("pos").orElseThrow();
            CompoundTag stateTag = entry.getCompound("state").orElseThrow();
            states[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, stateTag);
            if (!NbtUtils.writeBlockState(states[i]).equals(stateTag)) {
                throw new IllegalStateException(
                        "Invalid pending faller block state at " + BlockPos.of(positions[i]));
            }
        }
        chunk.hbm$setPendingFallers(positions, states, size);
    }
}

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
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class BulkHistory {
    private final Long2ObjectLinkedOpenHashMap<Snapshot> before;
    private final Long2ObjectLinkedOpenHashMap<Snapshot> after;

    BulkHistory(
            Long2ObjectLinkedOpenHashMap<Snapshot> before,
            Long2ObjectLinkedOpenHashMap<Snapshot> after) {
        this.before = before;
        this.after = after;
    }

    private static void restoreOwnership(ServerLevel level, BlockPos pos, Snapshot snapshot) {
        if (snapshot.claim() == MultiblockCoreIndex.NO_ENTRY) {
            return;
        }
        BlockPos owner =
                BlockPos.of(
                        MultiblockCoreIndex.corePacked(
                                snapshot.claim(), pos.getX(), pos.getY(), pos.getZ()));
        var chunk = level.getChunkSource().getChunkNow(owner.getX() >> 4, owner.getZ() >> 4);
        if (chunk != null
                && BuiltInRegistries.BLOCK.getId(chunk.getBlockState(owner).getBlock())
                        == snapshot.ownerId()) {
            BulkAction.claimed(
                    level, owner, pos, MultiblockCoreIndex.populationOf(snapshot.claim()));
        }
    }

    private static CompoundTag withoutReturnedItems(
            ServerLevel level, BlockPos pos, Snapshot saved) {
        CompoundTag nativeTag = BulkSnapshot.forPlacement(saved.tag(), pos);
        BlockEntity copy =
                BlockEntity.loadStatic(pos, saved.state(), nativeTag, level.registryAccess());
        BulkDispatch.forgetDrops(BulkRegistry.kind(copy), copy);
        var tag = copy.saveWithFullMetadata(level.registryAccess());
        BulkSnapshot.relativeData(tag, pos, saved.tag());
        return tag;
    }

    public Long2ObjectLinkedOpenHashMap<Snapshot> before() {
        return before;
    }

    public Long2ObjectLinkedOpenHashMap<Snapshot> after() {
        return after;
    }

    public void restore(ServerLevel level, boolean undo, boolean creative) {
        var snapshots = undo ? before : after;
        for (var entry : snapshots.long2ObjectEntrySet()) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            Snapshot snapshot = entry.getValue();
            if (!creative && !isAffected(level, pos)) {
                continue;
            }
            level.setBlock(
                    pos,
                    snapshot.state(),
                    Block.UPDATE_CLIENTS
                            | Block.UPDATE_KNOWN_SHAPE
                            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            if (snapshot.tag() != null) {
                BulkAction.imported(level, pos, snapshot.tag());
            }
            restoreOwnership(level, pos, snapshot);
        }
    }

    private boolean isAffected(ServerLevel level, BlockPos pos) {
        if (BulkAction.touched(level, pos.asLong())) {
            return true;
        }
        Snapshot source = after.get(pos.asLong());
        if (source.claim() == MultiblockCoreIndex.NO_ENTRY) {
            source = before.get(pos.asLong());
        }
        if (source.claim() == MultiblockCoreIndex.NO_ENTRY) {
            return false;
        }
        long owner =
                MultiblockCoreIndex.corePacked(source.claim(), pos.getX(), pos.getY(), pos.getZ());
        return BulkAction.touched(level, owner);
    }

    public void forgetReturnedItems(ServerLevel level, boolean undo, BulkHistory current) {
        var returned = undo ? after : before;
        for (var entry : current.before.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            Snapshot saved = entry.getValue();
            if (!returned.containsKey(key) || saved.tag() == null) continue;
            if (!saved.state().hasBlockEntity()) continue;
            if (saved.state().is(level.getBlockState(BlockPos.of(key)).getBlock())) continue;
            CompoundTag tag = withoutReturnedItems(level, BlockPos.of(key), saved);
            returned.put(key, new Snapshot(saved.state(), tag, saved.claim(), saved.ownerId()));
        }
    }

    public record Snapshot(BlockState state, @Nullable CompoundTag tag, long claim, int ownerId) {
        Snapshot(BlockState state, @Nullable CompoundTag tag) {
            this(state, tag, MultiblockCoreIndex.NO_ENTRY, -1);
        }
    }
}

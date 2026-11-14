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

package com.hbm.fabric.integration.bulk;

import com.hbm.packet.BulkTransfers;
import com.hbm.packet.layout.BulkPasteLayout;
import com.hbm.world.bulk.BulkPasteSessions;
import com.hbm.world.bulk.BulkSession;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class BulkPasteQueue implements BulkPasteLayout.Blocks {
    private final long session = BulkSession.nextClientId();
    private final LongArrayList positions = new LongArrayList();
    private final ObjectArrayList<BlockState> states = new ObjectArrayList<>();
    private final ObjectArrayList<CompoundTag> tags = new ObjectArrayList<>();
    private boolean opened;

    public void add(
            Level schematic, LevelChunk chunk, BlockPos pos, BlockState state, boolean nativeNbt) {
        CompoundTag tag =
                ((BulkSchematicData) chunk)
                        .hbm$bulkSnapshot(
                                schematic,
                                pos,
                                state.hasBlockEntity() ? chunk.getBlockEntity(pos) : null);
        if (tag != null) {
            tag = LitematicaBulkData.forPaste(tag, state, nativeNbt, schematic.registryAccess());
        }
        positions.add(pos.asLong());
        states.add(state);
        tags.add(tag);
        if (positions.size() == BulkPasteSessions.MAX_BLOCKS) flush(false);
    }

    public void flush(boolean finished) {
        if (positions.isEmpty() && (!finished || !opened)) return;
        BulkTransfers.sendPaste(BulkPasteLayout.encode(session, finished, this));
        positions.clear();
        states.clear();
        tags.clear();
        opened = !finished;
    }

    @Override
    public int count() {
        return positions.size();
    }

    @Override
    public long position(int index) {
        return positions.getLong(index);
    }

    @Override
    public int stateId(int index) {
        return Block.getId(states.get(index));
    }

    @Override
    public boolean tagPresent(int index) {
        return tags.get(index) != null;
    }

    @Override
    public CompoundTag tag(int index) {
        return tags.get(index);
    }

    public void cancel() {
        positions.clear();
        states.clear();
        tags.clear();
        flush(true);
    }
}

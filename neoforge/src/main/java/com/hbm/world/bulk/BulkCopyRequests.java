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

import com.hbm.packet.BulkTransfers;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.layout.BulkCopyLayout;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class BulkCopyRequests {
    private static final Long2ObjectOpenHashMap<Receiver> PENDING = new Long2ObjectOpenHashMap<>();
    private static long sequence;

    private BulkCopyRequests() {}

    public static long register(Receiver receiver) {
        long id = ++sequence;
        PENDING.put(id, receiver);
        return id;
    }

    public static void accept(long id, boolean success, CompoundTag data, boolean finished) {
        Receiver receiver = finished ? PENDING.remove(id) : PENDING.get(id);
        if (receiver != null) {
            receiver.accept(success, data, finished);
        }
    }

    public static void serve(ServerPlayer player, long request, long chunkKey) {
        var level = player.level();
        var chunk =
                level.getChunkSource()
                        .getChunkNow(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey));
        if (chunk == null || !ChunkTrackerIndex.canRepair(player, chunkKey)) {
            BulkTransfers.sendCopy(
                    BulkCopyLayout.encode(request, false, new CompoundTag(), true), player);
            return;
        }
        CompoundTag data = new CompoundTag();
        boolean full =
                Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)
                        .test(player.createCommandSourceStack());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        var sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        int count = 0;
        for (int section = 0; section < sections.length; section++) {
            if (!sections[section].maybeHas(BulkAction::owns)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(section) << 4;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        if (!BulkAction.owns(sections[section].getBlockState(x, y, z))) continue;
                        pos.set(baseX + x, baseY + y, baseZ + z);
                        CompoundTag tag = BulkSnapshot.capture(level, pos);
                        if (tag == null) continue;
                        if (!full) tag = BulkSnapshot.publicData(tag);
                        if (tag.isEmpty()) continue;
                        data.put(Long.toString(pos.asLong()), tag);
                        if (++count == BulkPasteSessions.MAX_BLOCKS) {
                            BulkTransfers.sendCopy(
                                    BulkCopyLayout.encode(request, true, data, false), player);
                            data = new CompoundTag();
                            count = 0;
                        }
                    }
        }
        BulkTransfers.sendCopy(BulkCopyLayout.encode(request, true, data, true), player);
    }

    public static void disconnected() {
        PENDING.clear();
    }

    public static void cancel(long id) {
        PENDING.remove(id);
    }

    @FunctionalInterface
    public interface Receiver {
        void accept(boolean success, CompoundTag data, boolean finished);
    }
}

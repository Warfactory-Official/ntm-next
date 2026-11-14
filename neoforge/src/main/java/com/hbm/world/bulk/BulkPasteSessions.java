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

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class BulkPasteSessions {
    public static final int MAX_BLOCKS = 256;
    private static final Map<ServerPlayer, Long2ObjectOpenHashMap<Session>> SESSIONS =
            new WeakHashMap<>();

    private BulkPasteSessions() {}

    public static BulkSession session(ServerPlayer player, long id) {
        return getOrCreate(player, id).bulk;
    }

    private static Session getOrCreate(ServerPlayer player, long id) {
        var sessions = SESSIONS.computeIfAbsent(player, ignored -> new Long2ObjectOpenHashMap<>());
        return sessions.computeIfAbsent(id, ignored -> new Session());
    }

    public static void paste(
            ServerPlayer player,
            long id,
            boolean finished,
            long[] positions,
            BlockState[] states,
            @Nullable CompoundTag[] tags) {
        if (!Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)
                .test(player.createCommandSourceStack())) return;
        var level = player.level();
        for (int i = 0; i < positions.length; i++) {
            BlockPos pos = BlockPos.of(positions[i]);
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null
                    || level.isOutsideBuildHeight(pos)
                    || !level.getWorldBorder().isWithinBounds(pos))
                throw new IllegalArgumentException("Bulk paste target is not loaded: " + pos);
            if (!BulkAction.owns(states[i]) && !BulkAction.owns(chunk.getBlockState(pos)))
                throw new IllegalArgumentException("Bulk paste requires an NTM block at " + pos);
        }
        var actionSession = session(player, id);
        for (long pos : positions) actionSession.recordWrite(level, pos);
        try (var bulk = BulkAction.begin(actionSession)) {
            for (int i = 0; i < positions.length; i++) {
                BlockPos pos = BlockPos.of(positions[i]);
                BulkAction.record(level, pos, level.getBlockState(pos), states[i]);
                level.setBlock(
                        pos,
                        states[i],
                        Block.UPDATE_CLIENTS
                                | Block.UPDATE_KNOWN_SHAPE
                                | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
                if (tags[i] != null) BulkAction.imported(level, pos, tags[i]);
            }
        }
        if (finished) finish(player, id);
    }

    public static void disconnected(ServerPlayer player) {
        var sessions = SESSIONS.remove(player);
        if (sessions != null) {
            for (Session session : sessions.values()) {
                session.bulk.clear();
            }
        }
    }

    public static void finish(ServerPlayer player, long id) {
        var sessions = SESSIONS.get(player);
        if (sessions == null) return;
        Session removed = sessions.remove(id);
        if (removed != null) removed.bulk.clear();
        if (sessions.isEmpty()) SESSIONS.remove(player);
    }

    public static void received(ServerPlayer player, long id, int section) {
        if (section < 0) throw new IllegalArgumentException("Negative bulk section index");
        getOrCreate(player, id).received.add(section);
    }

    public static void end(ServerPlayer player, long id, int count) {
        if (count < 0) throw new IllegalArgumentException("Negative bulk section count");
        getOrCreate(player, id).expected = count;
        complete(player, id);
    }

    public static void complete(ServerPlayer player, long id) {
        var sessions = SESSIONS.get(player);
        Session session = sessions == null ? null : sessions.get(id);
        if (session != null && session.expected >= 0 && session.received.size() == session.expected)
            finish(player, id);
    }

    private static final class Session {
        final BulkSession bulk = new BulkSession();
        final IntOpenHashSet received = new IntOpenHashSet();
        int expected = -1;
    }
}

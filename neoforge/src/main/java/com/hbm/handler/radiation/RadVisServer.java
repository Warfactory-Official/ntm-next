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

package com.hbm.handler.radiation;

import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RadVisSnapshotLayout;
import com.hbm.platform.ServerEvents;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.Commands;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import static com.hbm.handler.radiation.RadiationSystemNT.KIND_UNI;

public final class RadVisServer {
    public static final int MAX_RADIUS = 8;
    static final int PERIOD = 5;
    static final int LAYOUT_BUDGET = 256 * 1024;

    private static final Map<UUID, Watch> WATCHES = new HashMap<>();

    private RadVisServer() {}

    public static void init() {
        final Consumer<ServerPlayer> callback = player -> WATCHES.remove(player.getUUID());
        ServerEvents.DISCONNECT.add(callback);
        final Consumer<MinecraftServer> callback2 = server -> WATCHES.clear();
        ServerEvents.STOPPING.add(callback2);
    }

    public static boolean allowed(ServerPlayer player) {
        return player.level().getServer().isSingleplayerOwner(player.nameAndId())
                || Commands.LEVEL_GAMEMASTERS.check(player.permissions());
    }

    public static void watch(ServerPlayer player, int radius) {
        WATCHES.remove(player.getUUID());
        if (radius < 0) return;
        WATCHES.put(player.getUUID(), new Watch(Math.min(radius, MAX_RADIUS)));
        if (!allowed(player))
            player.sendSystemMessage(Component.translatable("commands.hbm.radvis.denied"));
    }

    static void publish(MinecraftServer server) {
        if (WATCHES.isEmpty()) return;
        long tick = server.getTickCount();
        for (Iterator<Map.Entry<UUID, Watch>> it = WATCHES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Watch> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            Watch watch = entry.getValue();
            if (!allowed(player)) {
                watch.sent.clear();
                watch.dimension = null;
                continue;
            }
            if (tick < watch.nextTick) continue;
            watch.nextTick = tick + PERIOD;
            ServerLevel level = player.level();
            RadVisSnapshot snap =
                    RadVisSnapshot.capture(
                            level,
                            SectionPos.blockToSectionCoord(player.getBlockX()),
                            SectionPos.blockToSectionCoord(player.getBlockZ()),
                            watch.radius);
            if (snap == null) continue;
            if (level.dimension() != watch.dimension) {
                watch.sent.clear();
                watch.dimension = level.dimension();
            }
            PacketWire.sendTo(RadVisSnapshotLayout.encode(snap, layouts(snap, watch.sent)), player);
        }
    }

    static BitSet layouts(RadVisSnapshot snap, Long2IntOpenHashMap sent) {
        BitSet layout = new BitSet(snap.size());
        int budget = LAYOUT_BUDGET;
        for (int i = 0; i < snap.size(); i++) {
            RadVisSnapshot.Section sec = snap.at(i);
            if (sec == null || sec.kind() == KIND_UNI) continue;
            long key = snap.sectionKey(i);
            if (sent.get(key) == sec.revision()) continue;
            int bytes = RadVisSnapshot.layoutBytes(sec.pocketCount());
            if (bytes > budget) continue;
            budget -= bytes;
            layout.set(i);
            sent.put(key, sec.revision());
        }
        sent.keySet().removeIf((long key) -> !snap.inWindow(SectionPos.x(key), SectionPos.z(key)));
        return layout;
    }

    private static final class Watch {
        final int radius;
        final Long2IntOpenHashMap sent = new Long2IntOpenHashMap();
        long nextTick;
        @Nullable ResourceKey<Level> dimension;

        Watch(int radius) {
            this.radius = radius;
        }
    }
}

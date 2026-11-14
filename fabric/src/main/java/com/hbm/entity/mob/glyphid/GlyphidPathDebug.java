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

package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.packet.ClientEventConstants;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.GlyphidPathLayout;
import com.hbm.platform.Network;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.pathfinder.Path;

public final class GlyphidPathDebug {

    public static final int BROADCAST_INTERVAL = 5;
    public static final int MAX_PATH_NODES = ClientEventConstants.GLYPHID_PATH_MAX_NODES;

    private static final Set<UUID> WATCHERS = ConcurrentHashMap.newKeySet();

    private GlyphidPathDebug() {}

    public static boolean toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        if (WATCHERS.remove(id)) {
            PacketWire.sendTo(GlyphidPathLayout.encode(-1, 0, 0, 0, 0, null), player);
            return false;
        }
        WATCHERS.add(id);
        return true;
    }

    public static boolean hasWatchers() {
        return !WATCHERS.isEmpty();
    }

    public static void broadcast(GlyphidEntity glyphid) {
        if (WATCHERS.isEmpty()) return;

        Path path = glyphid.getNavigation().getPath();
        if (path == null || path.getNodeCount() == 0) return;

        var target = path.getTarget();
        ArrayList<ServerPlayer> receivers = new ArrayList<>();
        for (ServerPlayer player : Network.playersWatchingEntity(glyphid)) {
            if (WATCHERS.contains(player.getUUID())) receivers.add(player);
        }
        if (!receivers.isEmpty())
            PacketWire.sendToPlayers(
                    GlyphidPathLayout.encode(
                            glyphid.getId(),
                            path.getNextNodeIndex(),
                            target.getX(),
                            target.getY(),
                            target.getZ(),
                            path),
                    receivers);
    }
}

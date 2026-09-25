// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.packet.toclient.GlyphidPathPayload;
import com.hbm.platform.Services;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.pathfinder.Path;

public final class GlyphidPathDebug {

    public static final int BROADCAST_INTERVAL = 5;

    private static final Set<UUID> WATCHERS = ConcurrentHashMap.newKeySet();

    private GlyphidPathDebug() {}

    public static boolean toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        if (WATCHERS.remove(id)) {
            Services.NETWORK.sendTo(GlyphidPathPayload.cleared(-1), player);
            return false;
        }
        WATCHERS.add(id);
        return true;
    }

    public static boolean hasWatchers() {
        return !WATCHERS.isEmpty();
    }

    public static void broadcast(EntityGlyphid glyphid) {
        if (WATCHERS.isEmpty()) return;

        Path path = glyphid.getNavigation().getPath();
        if (path == null || path.getNodeCount() == 0) return;

        GlyphidPathPayload payload = GlyphidPathPayload.of(glyphid.getId(), path);

        for (ServerPlayer player : Services.NETWORK.playersWatchingEntity(glyphid)) {
            if (WATCHERS.contains(player.getUUID())) Services.NETWORK.sendTo(payload, player);
        }
    }
}

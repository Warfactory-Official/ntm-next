// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.interfaces.injected.FarViewer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class FarTracking {
    private FarTracking() {}

    public static int range(ServerPlayer player) {
        return Math.min(
                ((FarViewer) player.connection).hbm$farView(),
                NeoForgeHbmConfig.runtime().farTrackingRange());
    }

    public static boolean anyFarViewer(Entity entity) {
        for (ServerPlayer player : ((ServerLevel) entity.level()).players()) {
            double range = range(player),
                    dx = entity.getX() - player.getX(),
                    dz = entity.getZ() - player.getZ();
            if (dx * dx + dz * dz < range * range) return true;
        }
        return false;
    }
}

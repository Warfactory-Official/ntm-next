// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle.helper;

import com.hbm.particle.HbmParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class VolcanoSmokeCreator {

    private static final double RANGE_SQ = 250D * 250D;

    public static void composeEffect(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel server)) return;
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(x, y, z) > RANGE_SQ) continue;
            server.sendParticles(
                    player, HbmParticles.VOLCANO_SMOKE.get(), true, true, x, y, z, 1, 0, 0, 0, 0);
        }
    }
}

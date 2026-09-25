// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle.helper;

import com.hbm.particle.FlameParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class FlameCreator {

    public static final int META_FIRE = 0;
    public static final int META_BALEFIRE = 1;
    public static final int META_DIGAMMA = 2;
    public static final int META_OXY = 3;
    public static final int META_BLACK = 4;

    private static final double RANGE_SQ = 50D * 50D;

    public static void composeEffect(Level level, double x, double y, double z, int meta) {
        if (!(level instanceof ServerLevel server)) return;
        FlameParticleOptions options = new FlameParticleOptions(meta);
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(x, y, z) > RANGE_SQ) continue;
            server.sendParticles(player, options, true, true, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void composeEffectClient(Level level, double x, double y, double z, int meta) {
        level.addParticle(new FlameParticleOptions(meta), true, false, x, y, z, 0, 0, 0);
    }
}

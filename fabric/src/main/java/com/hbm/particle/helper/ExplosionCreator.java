// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle.helper;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.particle.HbmEffectNT;
import com.hbm.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ExplosionCreator {

    private ExplosionCreator() {}

    public static void composeEffectSmall(Level level, double x, double y, double z) {
        send(level, x, y, z, HbmEffectNT.ExplosionSmall, 300);
    }

    public static void composeEffectStandard(Level level, double x, double y, double z) {
        send(level, x, y, z, HbmEffectNT.ExplosionStandard, 300);
    }

    public static void composeEffectLarge(Level level, double x, double y, double z) {
        send(level, x, y, z, HbmEffectNT.ExplosionLarge, 350);
    }

    public static void composeEffectRBMKMush(
            Level level, double x, double y, double z, float scale) {
        send(level, x, y, z, HbmEffectNT.RBMKMush, 250, scale);
    }

    private static void send(
            Level level, double x, double y, double z, HbmEffectNT effect, int range) {
        send(level, x, y, z, effect, range, 0F);
    }

    private static void send(
            Level level, double x, double y, double z, HbmEffectNT effect, int range, float scale) {
        if (!(level instanceof ServerLevel server)) return;
        Services.NETWORK.sendToAllAround(
                new EffectNTPayload(effect, x, y, z, scale),
                new TargetPoint(server, x, y, z, range));
    }
}

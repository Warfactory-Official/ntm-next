// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle.helper;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.ExplosionSmallPayload;
import com.hbm.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ExplosionSmallCreator {

    private ExplosionSmallCreator() {}

    public static void composeEffect(
            Level level,
            double x,
            double y,
            double z,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMult) {
        if (!(level instanceof ServerLevel server)) return;
        Services.NETWORK.sendToAllAround(
                new ExplosionSmallPayload(x, y, z, cloudCount, cloudScale, cloudSpeedMult, 15),
                new TargetPoint(server, x, y, z, 200));
    }
}

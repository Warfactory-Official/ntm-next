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

package com.hbm.client;

import com.hbm.api.environment.Environment;
import com.hbm.entity.missile.Missile;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class MissileExhaustEffects {

    private MissileExhaustEffects() {}

    public static void plume(
            Level level,
            double x,
            double y,
            double z,
            double sx,
            double sy,
            double sz,
            float scale,
            double vx,
            double vy,
            double vz) {
        var viewer = Minecraft.getInstance().player;
        if (viewer == null) return;
        double eyeX = viewer.getX(), eyeY = viewer.getEyeY(), eyeZ = viewer.getZ();
        double lengthSq = sx * sx + sy * sy + sz * sz;
        double length = Math.sqrt(lengthSq);
        double bx = 0D, by = 0D, bz = 0D;
        if (lengthSq >= 1.0E-8D) {
            bx = -sx / length;
            by = -sy / length;
            bz = -sz / length;
        }
        double pressure = Double.NaN;
        for (int i = 0, puffs = Missile.puffCount(length); i < puffs; i++) {
            double back = length - i;
            double px = x + bx * back, py = y + by * back, pz = z + bz * back;
            if (!Missile.withinExhaustRange(px - eyeX, py - eyeY, pz - eyeZ)) continue;
            if (Double.isNaN(pressure)) pressure = Environment.particlePressure(level);
            ClientEffects.spawnRocketFlameInAir(
                    level,
                    px,
                    py,
                    pz,
                    scale,
                    vx,
                    vy,
                    vz,
                    60 + level.getRandom().nextInt(20),
                    pressure);
        }
    }
}

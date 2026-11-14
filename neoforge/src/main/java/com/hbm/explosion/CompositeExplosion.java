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

package com.hbm.explosion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class CompositeExplosion {
    public static final int CLOUD = 1;
    public static final int RUBBLE = 2;
    public static final int SHRAPNEL = 4;

    private CompositeExplosion() {}

    public static void detonate(
            Level level, double x, double y, double z, float strength, int dressing) {
        detonate(level, null, x, y, z, strength, false, dressing);
    }

    public static void incendiary(
            Level level, double x, double y, double z, float strength, int dressing) {
        detonate(level, null, x, y, z, strength, true, dressing);
    }

    public static void detonate(
            Level level,
            @Nullable Entity source,
            double x,
            double y,
            double z,
            float strength,
            boolean fire,
            int dressing) {
        var server = (ServerLevel) level;
        server.explode(source, x, y, z, strength, fire, Level.ExplosionInteraction.BLOCK);
        int whole = (int) strength;
        if ((dressing & CLOUD) != 0) {
            BlastSmoke.cloud(server, x, y, z, (int) (850 * (1 - Math.exp(-(whole / 15))) + 15));
        }
        if ((dressing & RUBBLE) != 0) BlastDebris.rubble(server, x, y, z, whole / 10);
        if ((dressing & SHRAPNEL) != 0) BlastDebris.shrapnel(server, x, y, z, whole / 3);
    }

    public static void bust(
            Level level,
            double x,
            double y,
            double z,
            Vec3 direction,
            float strength,
            double depth) {
        var server = (ServerLevel) level;
        var unit = direction.normalize();
        for (int i = 0; i < depth; i += 2) {
            server.explode(
                    null,
                    x + unit.x * i,
                    y + unit.y * i,
                    z + unit.z * i,
                    strength,
                    false,
                    Level.ExplosionInteraction.BLOCK);
        }
    }
}

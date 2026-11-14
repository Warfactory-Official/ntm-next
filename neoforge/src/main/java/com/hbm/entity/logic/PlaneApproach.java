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

package com.hbm.entity.logic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

final class PlaneApproach {

    private static final double RUN_UP = 100D;

    private PlaneApproach() {}

    static void lineUp(
            PlaneBaseEntity plane,
            RandomSource random,
            double x,
            double y,
            double z,
            double speed,
            double altitude) {
        Vec3 heading =
                new Vec3(random.nextDouble() - 0.5D, 0D, random.nextDouble() - 0.5D)
                        .normalize()
                        .scale(speed);
        plane.snapTo(x - heading.x * RUN_UP, y + altitude, z - heading.z * RUN_UP, 0F, 0F);
        plane.setDeltaMovement(heading);
        plane.rotation();
        plane.forceSpawnChunk();
    }
}

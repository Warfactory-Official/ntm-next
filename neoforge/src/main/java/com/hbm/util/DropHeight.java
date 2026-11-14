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

package com.hbm.util;

import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

public final class DropHeight {

    private static final int CLEARANCE = 32;

    private DropHeight() {}

    public static double clearOfTerrain(LevelReader level, double legacy, double x, double z) {
        return Math.max(
                legacy,
                level.getHeight(
                                Heightmap.Types.MOTION_BLOCKING,
                                (int) Math.floor(x),
                                (int) Math.floor(z))
                        + CLEARANCE);
    }
}

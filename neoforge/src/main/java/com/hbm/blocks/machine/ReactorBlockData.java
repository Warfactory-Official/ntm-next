/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.machine;

import com.hbm.world.phys.SilhouetteMesh;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ReactorBlockData {

    public static final int[][][] TORUS_LAYOUT = {
        {
            {0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0},
            {0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
            {0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
            {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
            {3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
            {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
            {0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
            {0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
            {0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0}
        },
        {
            {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
            {0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
            {1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
            {1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
            {3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
            {3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
            {3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
            {1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
            {1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
            {0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
            {0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0}
        },
        {
            {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
            {0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
            {0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
            {1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
            {1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
            {3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
            {3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
            {3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
            {1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
            {1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
            {0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
            {0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
            {0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0}
        }
    };

    public static final VoxelShape[] CHANNEL_SELECTION = new VoxelShape[16];
    public static final VoxelShape[] CHANNEL_COLLISION = new VoxelShape[16];
    public static final VoxelShape[] OUTLET_BY_DIRECTION = new VoxelShape[Direction.VALUES.length];

    public static final SilhouetteMesh TORUS_LEVEL =
            SilhouetteMesh.octagonalPrism(
                    new float[] {
                        8F, 0F, 3.6066017F,
                        3.6066017F, 0F, 8F,
                        -2.6066017F, 0F, 8F,
                        -7F, 0F, 3.6066017F,
                        -7F, 0F, -2.6066017F,
                        -2.6066017F, 0F, -7F,
                        3.6066017F, 0F, -7F,
                        8F, 0F, -2.6066017F,
                        8F, 5F, 3.6066017F,
                        3.6066017F, 5F, 8F,
                        -2.6066017F, 5F, 8F,
                        -7F, 5F, 3.6066017F,
                        -7F, 5F, -2.6066017F,
                        -2.6066017F, 5F, -7F,
                        3.6066017F, 5F, -7F,
                        8F, 5F, -2.6066017F
                    },
                    new float[] {
                        0F, -1F, 0F, 0F,
                        0F, 1F, 0F, -5F,
                        0.70710677F, 0F, 0.70710677F, -8.2071066F,
                        0F, 0F, 1F, -8F,
                        -0.70710677F, 0F, 0.70710677F, -7.5F,
                        -1F, 0F, 0F, -7F,
                        -0.70710677F, 0F, -0.70710677F, -6.7928934F,
                        0F, 0F, -1F, -7F,
                        0.70710677F, 0F, -0.70710677F, -7.5F,
                        1F, 0F, 0F, -8F
                    });

    public static final SilhouetteMesh TORUS_TILTED =
            SilhouetteMesh.octagonalPrism(
                    new float[] {
                        8.1245966F, 0.34442213F, 2.941112F,
                        4.1915007F, -0.3490888F, 7.700702F,
                        -1.9040262F, -1.4238946F, 8.242218F,
                        -6.591307F, -2.2503889F, 4.2484484F,
                        -7.1245966F, -2.344422F, -1.941112F,
                        -3.191501F, -1.6509112F, -6.700702F,
                        2.904026F, -0.57610536F, -7.2422185F,
                        7.591307F, 0.2503888F, -3.2484481F,
                        7.2563558F, 5.2684608F, 2.941112F,
                        3.32326F, 4.5749497F, 7.700702F,
                        -2.772267F, 3.500144F, 8.242218F,
                        -7.459548F, 2.67365F, 4.2484484F,
                        -7.9928374F, 2.5796165F, -1.941112F,
                        -4.059742F, 3.2731276F, -6.700702F,
                        2.0357852F, 4.3479333F, -7.2422185F,
                        6.7230663F, 5.1744275F, -3.2484481F
                    },
                    new float[] {
                        0.17364818F, -0.98480773F, 0F, -1.0716318F,
                        -0.17364818F, 0.98480773F, 0F, -3.928368F,
                        0.7544065F, 0.13302222F, 0.64278764F, -8.065575F,
                        0.08583165F, 0.015134436F, 0.9961947F, -8.025879F,
                        -0.63302225F, -0.1116189F, 0.76604444F, -7.67813F,
                        -0.98106027F, -0.17298739F, 0.087155744F, -7.226035F,
                        -0.7544065F, -0.13302222F, -0.64278764F, -6.9344254F,
                        -0.08583165F, -0.015134436F, -0.9961947F, -6.974121F,
                        0.63302225F, 0.1116189F, -0.76604444F, -7.32187F,
                        0.98106027F, 0.17298739F, -0.087155744F, -7.773965F
                    });

    static {
        double lower = 5 / 16.0;
        double upper = 11 / 16.0;
        VoxelShape core = Shapes.box(lower, 0, lower, upper, 0.5, upper);
        VoxelShape[] arms = {
            Shapes.box(lower, 0, 0, upper, 0.5, lower),
            Shapes.box(lower, 0, upper, upper, 0.5, 1),
            Shapes.box(0, 0, lower, lower, 0.5, upper),
            Shapes.box(upper, 0, lower, 1, 0.5, upper)
        };
        CHANNEL_COLLISION[0] = core;
        for (int mask = 1; mask < 16; mask++) {
            int arm = Integer.lowestOneBit(mask);
            CHANNEL_COLLISION[mask] =
                    Shapes.or(
                            CHANNEL_COLLISION[mask ^ arm],
                            arms[Integer.numberOfTrailingZeros(arm)]);
        }
        for (int mask = 0; mask < 16; mask++) {
            CHANNEL_SELECTION[mask] =
                    Shapes.box(
                            (mask & 4) != 0 ? 0 : lower,
                            0,
                            (mask & 1) != 0 ? 0 : lower,
                            (mask & 8) != 0 ? 1 : upper,
                            0.5,
                            (mask & 2) != 0 ? 1 : upper);
        }
        OUTLET_BY_DIRECTION[Direction.NORTH.ordinal()] =
                Shapes.box(0.3125, 0, 0.625, 0.6875, 0.5, 1);
        OUTLET_BY_DIRECTION[Direction.SOUTH.ordinal()] =
                Shapes.box(0.3125, 0, 0, 0.6875, 0.5, 0.375);
        OUTLET_BY_DIRECTION[Direction.WEST.ordinal()] =
                Shapes.box(0.625, 0, 0.3125, 1, 0.5, 0.6875);
        OUTLET_BY_DIRECTION[Direction.EAST.ordinal()] =
                Shapes.box(0, 0, 0.3125, 0.375, 0.5, 0.6875);
    }

    private ReactorBlockData() {}
}

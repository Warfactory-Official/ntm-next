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

package com.hbm.blocks.network;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TransportBlockData {

    public static final int[] ROUTER_FACE_COLOURS = {
        0xFFFF0000, 0xFFFF8000, 0xFFFFFF00,
        0xFF00FF00, 0xFF0080FF, 0xFF8000FF
    };

    public static final VoxelShape CONVEYOR_SLAB = Shapes.box(0, 0, 0, 1, 0.25, 1);
    public static final VoxelShape PARTITIONER_SHAPE = Shapes.box(0, 0, 0, 1, 0.75, 1);

    public static final VoxelShape[][][] BOX_SELECTION = new VoxelShape[2][5][];
    public static final VoxelShape[][][] BOX_COLLISION = new VoxelShape[2][5][];
    public static final VoxelShape[] PNEUMATIC_SELECTION = new VoxelShape[64];
    public static final VoxelShape[] PNEUMATIC_COLLISION = new VoxelShape[64];

    static {
        for (int hub = 1; hub <= 2; hub++) {
            for (int size = 0; size < 5; size++) {
                prepareBoxed(size, hub);
            }
        }
        preparePneumatic();
    }

    private TransportBlockData() {}

    private static void prepareBoxed(int size, int hub) {
        double lower = (2 + size) / 16.0;
        double upper = (14 - size) / 16.0;
        double junctionLower = (hub + size) / 16.0;
        double junctionUpper = (16 - hub - size) / 16.0;

        VoxelShape core = Shapes.box(lower, lower, lower, upper, upper, upper);
        VoxelShape junction =
                Shapes.box(
                        junctionLower,
                        junctionLower,
                        junctionLower,
                        junctionUpper,
                        junctionUpper,
                        junctionUpper);
        VoxelShape alongX = Shapes.box(0, lower, lower, 1, upper, upper);
        VoxelShape alongY = Shapes.box(lower, 0, lower, upper, 1, upper);
        VoxelShape alongZ = Shapes.box(lower, lower, 0, upper, upper, 1);

        VoxelShape[] arms = {
            Shapes.box(lower, lower, 0, upper, upper, lower),
            Shapes.box(lower, lower, upper, upper, upper, 1),
            Shapes.box(lower, 0, lower, upper, lower, upper),
            Shapes.box(lower, upper, lower, upper, 1, upper),
            Shapes.box(0, lower, lower, lower, upper, upper),
            Shapes.box(upper, lower, lower, 1, upper, upper)
        };
        VoxelShape[] connected = new VoxelShape[64];
        connected[0] = Shapes.empty();
        for (int mask = 1; mask < 64; mask++) {
            int arm = Integer.lowestOneBit(mask);
            connected[mask] =
                    Shapes.joinUnoptimized(
                            connected[mask ^ arm],
                            arms[Integer.numberOfTrailingZeros(arm)],
                            BooleanOp.OR);
        }

        VoxelShape[] selection = new VoxelShape[64];
        VoxelShape[] collision = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            if (mask == 0) {
                selection[mask] = junction;
                collision[mask] = junction;
                continue;
            }
            if ((mask & 0b001111) == 0) {
                selection[mask] = alongX;
                collision[mask] = alongX;
                continue;
            }
            if ((mask & 0b110011) == 0) {
                selection[mask] = alongY;
                collision[mask] = alongY;
                continue;
            }
            if ((mask & 0b111100) == 0) {
                selection[mask] = alongZ;
                collision[mask] = alongZ;
                continue;
            }

            boolean bend = Integer.bitCount(mask) == 2;
            double centerLower = bend ? lower : junctionLower;
            double centerUpper = bend ? upper : junctionUpper;
            selection[mask] =
                    Shapes.box(
                            (mask & 16) != 0 ? 0 : centerLower,
                            (mask & 4) != 0 ? 0 : centerLower,
                            (mask & 1) != 0 ? 0 : centerLower,
                            (mask & 32) != 0 ? 1 : centerUpper,
                            (mask & 8) != 0 ? 1 : centerUpper,
                            (mask & 2) != 0 ? 1 : centerUpper);
            collision[mask] =
                    Shapes.joinUnoptimized(bend ? core : junction, connected[mask], BooleanOp.OR)
                            .optimize();
        }
        BOX_SELECTION[hub - 1][size] = selection;
        BOX_COLLISION[hub - 1][size] = collision;
    }

    private static void preparePneumatic() {
        double lower = 5 / 16.0;
        double upper = 11 / 16.0;
        VoxelShape core = Shapes.box(lower, lower, lower, upper, upper, upper);
        VoxelShape[] arms = new VoxelShape[6];
        arms[Direction.DOWN.ordinal()] = Shapes.box(lower, 0, lower, upper, lower, upper);
        arms[Direction.UP.ordinal()] = Shapes.box(lower, upper, lower, upper, 1, upper);
        arms[Direction.NORTH.ordinal()] = Shapes.box(lower, lower, 0, upper, upper, lower);
        arms[Direction.SOUTH.ordinal()] = Shapes.box(lower, lower, upper, upper, upper, 1);
        arms[Direction.WEST.ordinal()] = Shapes.box(0, lower, lower, lower, upper, upper);
        arms[Direction.EAST.ordinal()] = Shapes.box(upper, lower, lower, 1, upper, upper);

        PNEUMATIC_COLLISION[0] = core;
        for (int mask = 1; mask < 64; mask++) {
            int arm = Integer.highestOneBit(mask);
            PNEUMATIC_COLLISION[mask] =
                    Shapes.or(
                            PNEUMATIC_COLLISION[mask ^ arm],
                            arms[Integer.numberOfTrailingZeros(arm)]);
        }

        int west = 1 << Direction.WEST.ordinal();
        int down = 1 << Direction.DOWN.ordinal();
        int north = 1 << Direction.NORTH.ordinal();
        int east = 1 << Direction.EAST.ordinal();
        int up = 1 << Direction.UP.ordinal();
        int south = 1 << Direction.SOUTH.ordinal();
        for (int mask = 0; mask < 64; mask++) {
            PNEUMATIC_SELECTION[mask] =
                    Shapes.box(
                            (mask & west) != 0 ? 0 : lower,
                            (mask & down) != 0 ? 0 : lower,
                            (mask & north) != 0 ? 0 : lower,
                            (mask & east) != 0 ? 1 : upper,
                            (mask & up) != 0 ? 1 : upper,
                            (mask & south) != 0 ? 1 : upper);
        }
    }
}

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

import com.mojang.math.OctahedralGroup;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class Facing {

    private static final OctahedralGroup[][] QUARTER_TURNS = {
        {
            OctahedralGroup.IDENTITY,
            OctahedralGroup.BLOCK_ROT_X_90,
            OctahedralGroup.BLOCK_ROT_X_180,
            OctahedralGroup.BLOCK_ROT_X_270
        },
        {
            OctahedralGroup.IDENTITY,
            OctahedralGroup.BLOCK_ROT_Y_90,
            OctahedralGroup.BLOCK_ROT_Y_180,
            OctahedralGroup.BLOCK_ROT_Y_270
        },
        {
            OctahedralGroup.IDENTITY,
            OctahedralGroup.BLOCK_ROT_Z_90,
            OctahedralGroup.BLOCK_ROT_Z_180,
            OctahedralGroup.BLOCK_ROT_Z_270
        },
    };

    private static final byte[][] ROTATION = {
        {0, 1, 4, 5, 3, 2},
        {0, 1, 5, 4, 2, 3},
        {5, 4, 2, 3, 0, 1},
        {4, 5, 2, 3, 1, 0},
        {2, 3, 1, 0, 4, 5},
        {3, 2, 0, 1, 4, 5},
    };

    private Facing() {}

    public static Direction rotate(Direction dir, Direction axis) {
        return Direction.from3DDataValue(ROTATION[axis.ordinal()][dir.ordinal()]);
    }

    public static int yaw(Direction facing, int northYaw) {
        assert facing.getAxis() != Direction.Axis.Y
                : "a yaw is a horizontal quantity, asked for " + facing;
        return Math.floorMod(northYaw - 180 - 90 * facing.get2DDataValue(), 360);
    }

    public static int yawCcw(Direction facing, int northYaw) {
        assert facing.getAxis() != Direction.Axis.Y
                : "a yaw is a horizontal quantity, asked for " + facing;
        return Math.floorMod(northYaw + 180 + 90 * facing.get2DDataValue(), 360);
    }

    public static OctahedralGroup yawRotation(Direction facing, int northYaw) {
        return ry(yaw(facing, northYaw));
    }

    public static AABB rotateAabb(AABB box, Direction facing) {
        assert facing.getAxis() != Direction.Axis.Y
                : "a yaw is a horizontal quantity, asked for " + facing;
        return switch (facing) {
            case EAST -> new AABB(-box.maxZ, box.minY, box.minX, -box.minZ, box.maxY, box.maxX);
            case SOUTH -> new AABB(-box.maxX, box.minY, -box.maxZ, -box.minX, box.maxY, -box.minZ);
            case WEST -> new AABB(box.minZ, box.minY, -box.maxX, box.maxZ, box.maxY, -box.minX);
            default -> box;
        };
    }

    public static OctahedralGroup rx(int degrees) {
        return QUARTER_TURNS[0][quarter(degrees)];
    }

    public static OctahedralGroup ry(int degrees) {
        return QUARTER_TURNS[1][quarter(degrees)];
    }

    public static OctahedralGroup rz(int degrees) {
        return QUARTER_TURNS[2][quarter(degrees)];
    }

    public static Direction fromBombMeta(int meta) {
        return switch (meta) {
            case 2 -> Direction.EAST;
            case 3 -> Direction.WEST;
            case 5 -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
    }

    public static int toBombMeta(Direction dir) {
        return switch (dir) {
            case EAST -> 2;
            case WEST -> 3;
            case SOUTH -> 5;
            default -> 4;
        };
    }

    public static Direction[] shuffled() {
        var dirs = Direction.values();
        var random = ThreadLocalRandom.current();
        for (var i = dirs.length - 1; i > 0; i--) {
            var j = random.nextInt(i + 1);
            var swap = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = swap;
        }
        return dirs;
    }

    private static int quarter(int degrees) {
        assert Math.floorMod(degrees, 90) == 0
                : "a baked rotation must be a multiple of 90: " + degrees;
        return Math.floorMod(-degrees / 90, 4);
    }
}

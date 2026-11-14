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

package com.hbm.blocks.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class AxialSupportShape {

    private static final double WALL = 1.0D;

    private static final VoxelShape X = bore(Direction.Axis.X);
    private static final VoxelShape Y = bore(Direction.Axis.Y);
    private static final VoxelShape Z = bore(Direction.Axis.Z);

    private AxialSupportShape() {}

    static VoxelShape forAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }

    private static VoxelShape bore(Direction.Axis axis) {
        VoxelShape shape = Shapes.empty();
        for (Direction dir : Direction.VALUES) {
            if (dir.getAxis() == axis) continue;
            shape = Shapes.or(shape, slab(dir));
        }
        return shape;
    }

    private static VoxelShape slab(Direction dir) {
        double min =
                dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16.0D - WALL : 0.0D;
        double max = min + WALL;
        return switch (dir.getAxis()) {
            case X -> Block.box(min, 0.0D, 0.0D, max, 16.0D, 16.0D);
            case Y -> Block.box(0.0D, min, 0.0D, 16.0D, max, 16.0D);
            case Z -> Block.box(0.0D, 0.0D, min, 16.0D, 16.0D, max);
        };
    }
}

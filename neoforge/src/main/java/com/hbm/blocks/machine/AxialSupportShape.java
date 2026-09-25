// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

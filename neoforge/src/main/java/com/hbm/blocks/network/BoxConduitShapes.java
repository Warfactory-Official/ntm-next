// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BoxConduitShapes {

    public static final int DUCT_HUB = 1;
    public static final int CABLE_HUB = 2;
    private static final VoxelShape[][] SELECTION = new VoxelShape[10][];
    private static final VoxelShape[][] COLLISION = new VoxelShape[10][];

    private BoxConduitShapes() {}

    public static int maskOf(BlockState state) {
        int mask = 0;
        if (state.getValue(PipeBlock.EAST)) mask |= 32;
        if (state.getValue(PipeBlock.WEST)) mask |= 16;
        if (state.getValue(PipeBlock.UP)) mask |= 8;
        if (state.getValue(PipeBlock.DOWN)) mask |= 4;
        if (state.getValue(PipeBlock.SOUTH)) mask |= 2;
        if (state.getValue(PipeBlock.NORTH)) mask |= 1;
        return mask;
    }

    public static boolean maskHas(int mask, Direction dir) {
        return switch (dir) {
            case EAST -> (mask & 32) != 0;
            case WEST -> (mask & 16) != 0;
            case UP -> (mask & 8) != 0;
            case DOWN -> (mask & 4) != 0;
            case SOUTH -> (mask & 2) != 0;
            case NORTH -> (mask & 1) != 0;
        };
    }

    public static synchronized VoxelShape[] selection(int size, int hub) {
        assert size >= 0 && size < 5 && (hub == DUCT_HUB || hub == CABLE_HUB);
        int index = (hub - 1) * 5 + size;
        VoxelShape[] shapes = SELECTION[index];
        if (shapes == null) SELECTION[index] = shapes = buildSelection(size, hub);
        return shapes;
    }

    private static VoxelShape[] buildSelection(int size, int hub) {
        double l = (2 + size) / 16D, u = (14 - size) / 16D;
        double jl = (hub + size) / 16D, ju = (16 - hub - size) / 16D;
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            boolean pX = (mask & 32) != 0, nX = (mask & 16) != 0, pY = (mask & 8) != 0;
            boolean nY = (mask & 4) != 0, pZ = (mask & 2) != 0, nZ = (mask & 1) != 0;
            int count = Integer.bitCount(mask);
            if (mask == 0) {
                shapes[mask] = Shapes.box(jl, jl, jl, ju, ju, ju);
            } else if ((mask & 0b001111) == 0) {
                shapes[mask] = Shapes.box(0, l, l, 1, u, u);
            } else if ((mask & 0b110011) == 0) {
                shapes[mask] = Shapes.box(l, 0, l, u, 1, u);
            } else if ((mask & 0b111100) == 0) {
                shapes[mask] = Shapes.box(l, l, 0, u, u, 1);
            } else if (count == 2) {
                shapes[mask] =
                        Shapes.box(
                                nX ? 0 : l,
                                nY ? 0 : l,
                                nZ ? 0 : l,
                                pX ? 1 : u,
                                pY ? 1 : u,
                                pZ ? 1 : u);
            } else {
                shapes[mask] =
                        Shapes.box(
                                nX ? 0 : jl,
                                nY ? 0 : jl,
                                nZ ? 0 : jl,
                                pX ? 1 : ju,
                                pY ? 1 : ju,
                                pZ ? 1 : ju);
            }
        }
        return shapes;
    }

    public static synchronized VoxelShape[] collision(int size, int hub) {
        assert size >= 0 && size < 5 && (hub == DUCT_HUB || hub == CABLE_HUB);
        int index = (hub - 1) * 5 + size;
        VoxelShape[] shapes = COLLISION[index];
        if (shapes == null) COLLISION[index] = shapes = buildCollision(size, hub);
        return shapes;
    }

    private static VoxelShape[] buildCollision(int size, int hub) {
        double l = (2 + size) / 16D, u = (14 - size) / 16D;
        double jl = (hub + size) / 16D, ju = (16 - hub - size) / 16D;
        VoxelShape[] shapes = new VoxelShape[64];
        VoxelShape core = Shapes.box(l, l, l, u, u, u);
        VoxelShape junction = Shapes.box(jl, jl, jl, ju, ju, ju);
        VoxelShape x = Shapes.box(0, l, l, 1, u, u);
        VoxelShape y = Shapes.box(l, 0, l, u, 1, u);
        VoxelShape z = Shapes.box(l, l, 0, u, u, 1);
        VoxelShape[] arms = {
            Shapes.box(l, l, 0, u, u, l), Shapes.box(l, l, u, u, u, 1),
            Shapes.box(l, 0, l, u, l, u), Shapes.box(l, u, l, u, 1, u),
            Shapes.box(0, l, l, l, u, u), Shapes.box(u, l, l, 1, u, u)
        };
        VoxelShape[] connected = new VoxelShape[64];
        connected[0] = Shapes.empty();
        for (int mask = 1; mask < 64; mask++) {
            int last = Integer.lowestOneBit(mask);
            connected[mask] =
                    Shapes.joinUnoptimized(
                            connected[mask ^ last],
                            arms[Integer.numberOfTrailingZeros(last)],
                            BooleanOp.OR);
        }
        for (int mask = 0; mask < 64; mask++) {
            int count = Integer.bitCount(mask);
            if (mask == 0) {
                shapes[mask] = junction;
            } else if ((mask & 0b001111) == 0) {
                shapes[mask] = x;
            } else if ((mask & 0b110011) == 0) {
                shapes[mask] = y;
            } else if ((mask & 0b111100) == 0) {
                shapes[mask] = z;
            } else {
                shapes[mask] =
                        Shapes.joinUnoptimized(
                                        count != 2 ? junction : core, connected[mask], BooleanOp.OR)
                                .optimize();
            }
        }
        return shapes;
    }
}

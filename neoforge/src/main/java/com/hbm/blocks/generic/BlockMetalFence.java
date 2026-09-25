// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockMetalFence extends FenceBlock {

    public static final MapCodec<FenceBlock> CODEC = simpleCodec(BlockMetalFence::new);

    private static final double MIN = 6 / 16D;
    private static final double MAX = 10 / 16D;

    public BlockMetalFence(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<FenceBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        double minX = state.getValue(WEST) ? 0D : MIN;
        double maxX = state.getValue(EAST) ? 1D : MAX;
        double minZ = state.getValue(NORTH) ? 0D : MIN;
        double maxZ = state.getValue(SOUTH) ? 1D : MAX;
        return Shapes.box(minX, 0D, minZ, maxX, 1D, maxZ);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        boolean n = state.getValue(NORTH), s = state.getValue(SOUTH);
        boolean e = state.getValue(EAST), w = state.getValue(WEST);
        VoxelShape shape = Shapes.empty();

        if (n || s) {
            shape = Shapes.or(shape, Shapes.box(MIN, 0D, n ? 0D : MIN, MAX, 1D, s ? 1D : MAX));
        }

        if (e || w || (!n && !s)) {
            shape = Shapes.or(shape, Shapes.box(w ? 0D : MIN, 0D, MIN, e ? 1D : MAX, 1D, MAX));
        }
        return shape;
    }
}

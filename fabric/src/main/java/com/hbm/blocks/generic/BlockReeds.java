// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ISectionGeometry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockReeds extends Block implements ISectionGeometry {

    public static final MapCodec<BlockReeds> CODEC = simpleCodec(BlockReeds::new);

    private static final int MAX_DEPTH = 256;

    public BlockReeds(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BlockReeds> codec() {
        return CODEC;
    }

    @Override
    public boolean contextualGeometry() {
        return true;
    }

    public static int depth(BlockGetter level, BlockPos pos) {
        int depth = 1;
        BlockPos.MutableBlockPos below = pos.mutable();
        while (depth < MAX_DEPTH) {
            below.move(Direction.DOWN);
            if (below.getY() < level.getMinY() || !level.getBlockState(below).is(Blocks.WATER))
                break;
            depth++;
        }
        return depth;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.WATER);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }
}

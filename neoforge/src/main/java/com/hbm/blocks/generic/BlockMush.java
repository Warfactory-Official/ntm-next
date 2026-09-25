// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.RadiationConfig;
import com.hbm.data.WorldData;
import com.hbm.world.feature.HugeMushFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockMush extends BushBlock implements BonemealableBlock {

    private static final VoxelShape SHAPE = box(5.0D, 0.0D, 5.0D, 11.0D, 6.4D, 11.0D);

    private static final int SPREAD_CHANCE = 25;

    private static final int CROWD_REACH = 4;
    private static final int CROWD_LIMIT = 3;

    private static final int MYCELIUM_CHANCE = 5;

    private static final float BONEMEAL_CHANCE = 0.4F;

    public BlockMush(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                || growsOn(level, pos);
    }

    private static boolean growsOn(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlocks.WASTE_EARTH.get())
                || below.is(ModBlocks.WASTE_MYCELIUM.get())
                || below.is(ModBlocks.WASTE_TRINITITE.get())
                || below.is(ModBlocks.WASTE_TRINITITE_RED.get())
                || below.is(ModBlocks.BLOCK_WASTE.get())
                || below.is(ModBlocks.BLOCK_WASTE_PAINTED.get())
                || below.is(ModBlocks.BLOCK_WASTE_VITRIFIED.get());
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        if (WorldData.ENABLE_MYCELIUM.get()
                && level.getBlockState(pos.below()).is(ModBlocks.WASTE_EARTH.get())
                && random.nextInt(MYCELIUM_CHANCE) == 0) {
            level.setBlockAndUpdate(
                    pos.below(), ModBlocks.WASTE_MYCELIUM.get().defaultBlockState());
        }

        if (random.nextInt(SPREAD_CHANCE) != 0) return;

        int nearby = 0;
        for (int x = pos.getX() - CROWD_REACH; x <= pos.getX() + CROWD_REACH; x++) {
            for (int y = pos.getY() - 1; y <= pos.getY() + 1; y++) {
                for (int z = pos.getZ() - CROWD_REACH; z <= pos.getZ() + CROWD_REACH; z++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).is(this)
                            && ++nearby >= CROWD_LIMIT) {
                        return;
                    }
                }
            }
        }

        BlockPos target = wander(pos, random);
        for (int attempt = 0; attempt < 4; attempt++) {
            if (level.isEmptyBlock(target) && growsOn(level, target)) break;
            target = wander(target, random);
        }
        if (level.isEmptyBlock(target) && growsOn(level, target)) {
            level.setBlock(target, defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static BlockPos wander(BlockPos from, RandomSource random) {
        return new BlockPos(
                from.getX() + random.nextInt(5) - 2,
                from.getY() + random.nextInt(2) - random.nextInt(2),
                from.getZ() + random.nextInt(5) - 2);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return canSurvive(state, level, pos);
    }

    @Override
    public boolean isBonemealSuccess(
            Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < BONEMEAL_CHANCE;
    }

    @Override
    public void performBonemeal(
            ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.removeBlock(pos, false);
        HugeMushFeature.generate(level, pos);
    }
}

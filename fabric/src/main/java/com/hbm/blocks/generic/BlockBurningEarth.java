// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class BlockBurningEarth extends Block {

    private static final int SPREAD_CHANCE = 5;
    private static final int WALK_FIRE_SECONDS = 5;

    public BlockBurningEarth(Properties props) {
        super(props);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.igniteForSeconds(WALK_FIRE_SECONDS);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(
                ParticleTypes.FLAME,
                pos.getX() + random.nextFloat(),
                pos.getY() + 1.1,
                pos.getZ() + random.nextFloat(),
                0.0,
                0.0,
                0.0);
        level.addParticle(
                ParticleTypes.SMOKE,
                pos.getX() + random.nextFloat(),
                pos.getY() + 1.1,
                pos.getZ() + random.nextFloat(),
                0.0,
                0.0,
                0.0);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (level.isClientSide()) return;
        BlockState above = level.getBlockState(pos.above());
        if (!above.getFluidState().isEmpty() || above.isRedstoneConductor(level, pos.above())) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(SPREAD_CHANCE) == 0) spread(level, pos, random);

        level.setBlockAndUpdate(pos, ModBlocks.IMPACT_DIRT.get().defaultBlockState());
    }

    private void spread(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos ownAbove = pos.above();
        boolean raining = level.isRainingAt(pos);

        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                for (int k = -1; k < 2; k++) {
                    BlockPos at = pos.offset(i, j, k);
                    if (!level.isLoaded(at)) continue;
                    BlockState here = level.getBlockState(at);
                    BlockState over = level.getBlockState(at.above());

                    if (!over.canOcclude() && !raining && catches(here)) {
                        level.setBlockAndUpdate(at, defaultBlockState());
                    }
                    if (over.is(BlockTags.LEAVES) || over.getBlock() instanceof VegetationBlock) {
                        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                    }
                    if (here.is(ModBlocks.FROZEN_DIRT.get())) {
                        level.setBlockAndUpdate(at, Blocks.DIRT.defaultBlockState());
                    }

                    if (Services.PLATFORM.isFlammable(level, at.above(), over, Direction.UP)
                            && !over.is(BlockTags.LEAVES)
                            && !(over.getBlock() instanceof VegetationBlock)
                            && level.getBlockState(ownAbove).isAir()) {
                        level.setBlockAndUpdate(ownAbove, BaseFireBlock.getState(level, ownAbove));
                    }
                }
            }
        }
    }

    private static boolean catches(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(ModBlocks.WASTE_EARTH.get())
                || state.is(ModBlocks.FROZEN_GRASS.get())
                || state.is(ModBlocks.WASTE_MYCELIUM.get());
    }
}

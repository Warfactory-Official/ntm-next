// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class BlockFluidClassicBase extends LiquidBlock {

    protected BlockFluidClassicBase(ClassicFluid fluid, Properties props) {
        super(fluid, props);
    }

    public static int light(BlockState state, int quanta, int luminosity) {
        int level = state.getValue(LEVEL);
        int remaining = level == 0 ? quanta : level >= 8 ? quanta - 1 : (8 - level) * quanta / 8;
        return (remaining - 1) * luminosity / quanta;
    }

    @Override
    public ItemStack pickupBlock(
            @Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(LEVEL) != 0) return ItemStack.EMPTY;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL_IMMEDIATE);
        return ((ClassicFluid) fluid).spec().bucket().get();
    }

    protected boolean displaces(BlockState below) {
        return false;
    }

    protected void flowTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {}

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        flowTick(state, level, pos, random);
    }
}

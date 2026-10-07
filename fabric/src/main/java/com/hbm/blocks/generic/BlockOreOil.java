// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class BlockOreOil extends Block {

    public static final MapCodec<BlockOreOil> CODEC = simpleCodec(BlockOreOil::new);

    public BlockOreOil(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BlockOreOil> codec() {
        return CODEC;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighbor,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        FluidReserveBlock reserve = FluidReserveBlock.of(this);
        if (reserve != null) reserve.settle(level, pos, state);
    }
}

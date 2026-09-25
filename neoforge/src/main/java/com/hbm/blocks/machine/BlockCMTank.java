// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockCMTank extends Block {

    public static final MapCodec<BlockCMTank> CODEC = simpleCodec(BlockCMTank::new);

    public BlockCMTank(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BlockCMTank> codec() {
        return CODEC;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction direction) {
        return neighbour.getBlock() instanceof BlockCMTank
                || super.skipRendering(state, neighbour, direction);
    }
}

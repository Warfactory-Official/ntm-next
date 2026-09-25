// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ConveyorBlock extends ConveyorBendableBlock {

    public static final MapCodec<ConveyorBlock> CODEC = simpleCodec(ConveyorBlock::new);

    public ConveyorBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onSneakScrew(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(BEND) != ConveyorBend.RIGHT) {
            super.onSneakScrew(level, pos, state);
            return;
        }

        level.setBlock(
                pos,
                ModBlocks.CONVEYOR_LIFT
                        .get()
                        .defaultBlockState()
                        .setValue(FACING, state.getValue(FACING))
                        .setValue(ConveyorLiftBlock.PART, ConveyorLiftBlock.partAt(level, pos)),
                UPDATE_ALL);
    }
}

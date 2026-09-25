// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityChlorineSeal extends BlockEntity {

    private static final int MAX_STEPS = 50;

    public BlockEntityChlorineSeal(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHLORINE_SEAL.get(), pos, state);
    }

    public static void tick(
            Level level, BlockPos pos, BlockState state, BlockEntityChlorineSeal be) {
        spread(level, pos);
    }

    private static void spread(Level level, BlockPos start) {
        Block gas = ModBlocks.CHLORINE_GAS.get();
        Block seal = ModBlocks.VENT_CHLORINE_SEAL.get();
        RandomSource rand = level.getRandom();
        BlockPos.MutableBlockPos cursor = start.mutable();

        for (int step = 0; step <= MAX_STEPS; step++) {
            BlockState at = level.getBlockState(cursor);
            if (at.canBeReplaced()) {
                level.setBlockAndUpdate(cursor, gas.defaultBlockState());
                at = level.getBlockState(cursor);
            }

            if (!at.is(gas) && !at.is(seal)) return;
            cursor.move(Direction.from3DDataValue(rand.nextInt(6)));
        }
    }
}

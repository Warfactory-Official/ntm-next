// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CoalGasHandler {

    private CoalGasHandler() {}

    public static void onBlockBroken(Level level, BlockPos pos, BlockState broken) {
        if (level.isClientSide()) return;

        if (!broken.is(Blocks.COAL_ORE)
                && !broken.is(Blocks.DEEPSLATE_COAL_ORE)
                && !broken.is(Blocks.COAL_BLOCK)
                && !broken.is(ModBlocks.ORE_LIGNITE.get())) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            if (level.getRandom().nextInt(2) == 0 && level.getBlockState(at).isAir()) {
                level.setBlockAndUpdate(at, ModBlocks.GAS_COAL.get().defaultBlockState());
            }
        }
    }
}

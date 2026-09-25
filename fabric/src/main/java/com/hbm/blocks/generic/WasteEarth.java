// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WasteEarth extends Block {

    public WasteEarth(Properties props) {
        super(props);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();

        if (level.getBlockState(above).getBlock() instanceof MushroomBlock) {
            level.setBlockAndUpdate(above, ModBlocks.MUSH.get().defaultBlockState());
        }

        if (RadiationData.CLEANUP_DEAD_DIRT.get()
                || (level.getMaxLocalRawBrightness(above) < 4
                        && level.getBlockState(above).getLightDampening() > 2)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class HugeMushFeature {

    private HugeMushFeature() {}

    public static void generate(LevelWriter level, BlockPos o) {
        BlockState cap = ModBlocks.MUSH_BLOCK.get().defaultBlockState();
        BlockState stem = ModBlocks.MUSH_BLOCK_STEM.get().defaultBlockState();
        int flags = Block.UPDATE_ALL;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = -1; i < 2; i++)
            for (int j = -1; j < 2; j++) level.setBlock(pos.setWithOffset(o, i, 0, j), cap, flags);
        for (int i = -1; i < 2; i++)
            for (int j = -1; j < 2; j++) level.setBlock(pos.setWithOffset(o, i, 3, j), cap, flags);
        for (int i = -2; i < 3; i++)
            for (int j = -2; j < 3; j++) level.setBlock(pos.setWithOffset(o, i, 5, j), cap, flags);
        for (int i = -4; i < 5; i++)
            for (int j = -4; j < 5; j++)
                for (int k = 0; k < 3; k++)
                    level.setBlock(pos.setWithOffset(o, i, 6 + k, j), cap, flags);
        for (int i = -3; i < 4; i++)
            for (int j = -3; j < 4; j++) level.setBlock(pos.setWithOffset(o, i, 9, j), cap, flags);
        for (int i = -1; i < 2; i++)
            for (int j = -1; j < 2; j++) level.setBlock(pos.setWithOffset(o, i, 10, j), cap, flags);
        for (int i = 0; i < 8; i++) level.setBlock(pos.setWithOffset(o, 0, i, 0), stem, flags);
    }
}

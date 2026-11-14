/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.handler.pollution;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class PollutionWorldHandler {

    private PollutionWorldHandler() {}

    public static void destroyBlock(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT) {
            world.setBlock(
                    pos.immutable(), Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(BlockTags.LEAVES)
                || block == ModBlocks.LEAVES_LAYER.get()
                || block instanceof VegetationBlock
                || block == Blocks.SUGAR_CANE
                || block == Blocks.COCOA) {
            world.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}

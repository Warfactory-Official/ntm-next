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

package com.hbm.handler.radiation;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

final class RadiationWorldHandler {

    private RadiationWorldHandler() {}

    static void decayBlock(ServerLevel world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof DoublePlantBlock) {
            BlockPos base = pos.immutable();
            DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
            BlockPos lower = half == DoubleBlockHalf.LOWER ? base : base.below();
            BlockPos upper = half == DoubleBlockHalf.LOWER ? base.above() : base;
            world.setBlock(upper, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            world.setBlock(lower, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        if (block == Blocks.GRASS_BLOCK) {
            world.setBlock(
                    pos, ModBlocks.WASTE_EARTH.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        if (block instanceof TallGrassBlock) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        if (block instanceof LeavesBlock && block != ModBlocks.WASTE_LEAVES.get()) {
            if (world.getRandom().nextInt(7) <= 5) {
                world.setBlock(
                        pos,
                        ModBlocks.WASTE_LEAVES.get().defaultBlockState(),
                        Block.UPDATE_CLIENTS);
            } else {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}

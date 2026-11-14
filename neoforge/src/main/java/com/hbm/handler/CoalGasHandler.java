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

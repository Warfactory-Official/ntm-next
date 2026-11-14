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

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class PlantGrowth {

    private PlantGrowth() {}

    public static boolean supports(BlockState soil) {
        return soil.is(Blocks.GRASS_BLOCK)
                || soil.is(Blocks.DIRT)
                || soil.is(Blocks.COARSE_DIRT)
                || soil.is(Blocks.PODZOL)
                || soil.is(Blocks.FARMLAND)
                || soil.is(ModBlocks.DIRT_DEAD.get())
                || soil.is(ModBlocks.DIRT_OILY.get());
    }

    public static boolean hasWaterAtSoilSides(BlockGetter level, BlockPos lowerPlant) {
        int x = lowerPlant.getX();
        int soilY = lowerPlant.getY() - 1;
        int z = lowerPlant.getZ();
        var probe = new BlockPos.MutableBlockPos(x + 1, soilY, z);
        if (level.getBlockState(probe).is(Blocks.WATER)) {
            return true;
        }
        probe.set(x - 1, soilY, z);
        if (level.getBlockState(probe).is(Blocks.WATER)) {
            return true;
        }
        probe.set(x, soilY, z + 1);
        if (level.getBlockState(probe).is(Blocks.WATER)) {
            return true;
        }
        probe.set(x, soilY, z - 1);
        return level.getBlockState(probe).is(Blocks.WATER);
    }
}

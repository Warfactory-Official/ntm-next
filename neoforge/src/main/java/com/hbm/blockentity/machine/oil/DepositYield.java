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

package com.hbm.blockentity.machine.oil;

import com.hbm.blocks.ModBlocks;
import com.hbm.data.DataGroups.Param;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

record DepositYield(
        Param<Integer> oil,
        Param<Integer> gasMin,
        Param<Integer> gasMax,
        @Nullable Param<Double> drainChance) {
    void pump(Level level, BlockPos deposit, FluidTankNTM oilTank, FluidTankNTM gasTank) {
        RandomSource random = level.getRandom();
        oilTank.setFill((long) oilTank.getFill() + oil.get());
        int min = gasMin.get();
        gasTank.setFill((long) gasTank.getFill() + min + random.nextInt(gasMax.get() - min + 1));
        if (drainChance != null && random.nextDouble() < drainChance.get()) {
            level.setBlock(
                    deposit, ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}

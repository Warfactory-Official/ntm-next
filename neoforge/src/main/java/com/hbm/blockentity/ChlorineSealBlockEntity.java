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

package com.hbm.blockentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ChlorineSealBlockEntity extends BlockEntity implements BlockEntityTicks {

    private static final int MAX_STEPS = 50;

    public ChlorineSealBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHLORINE_SEAL.get(), pos, state);
    }

    @Override
    public void tickServer() {
        spread(level, worldPosition);
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

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

package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKDebrisDigammaBlock extends Block {
    private static final int SPREAD_DELAY = 2;

    public RBMKDebrisDigammaBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    private static boolean isCorruptible(Block b) {
        return b == ModBlocks.RBMK_DEBRIS.get()
                || b == ModBlocks.RBMK_DEBRIS_BURNING.get()
                || b == ModBlocks.RBMK_DEBRIS_RADIATING.get()
                || b == ModBlocks.CORIUM.get()
                || b == ModBlocks.BLOCK_CORIUM.get();
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (level instanceof ServerLevel server && !oldState.is(this)) schedule(server, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.VALUES) {
            BlockPos n = pos.relative(dir);
            if (isCorruptible(level.getBlockState(n).getBlock())) {
                level.setBlock(n, defaultBlockState(), 3);
            }
        }
    }

    private void schedule(ServerLevel level, BlockPos pos) {
        level.scheduleTick(pos, this, SPREAD_DELAY);
    }
}

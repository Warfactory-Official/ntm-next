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
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.botprime.BOTPrimeHeadEntity;
import com.hbm.items.ModItems;
import com.hbm.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BallsSpawnerBlock extends Block {
    private static final int DROP_HEIGHT = 300;

    public BallsSpawnerBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(ModItems.MECH_KEY.get())) return InteractionResult.PASS;
        stack.shrink(1);
        if (level instanceof ServerLevel server) {
            BOTPrimeHeadEntity bot =
                    ModEntities.BALLS_O_TRON.get().create(server, EntitySpawnReason.TRIGGERED);
            if (bot != null) {
                bot.snapTo(pos.getX() + 0.5, DROP_HEIGHT, pos.getZ() + 0.5, 0, 0);
                bot.setDeltaMovement(0, -1.0, 0);
                Platform.finalizeSpawn(
                        bot,
                        server,
                        server.getCurrentDifficultyAt(bot.blockPosition()),
                        EntitySpawnReason.TRIGGERED,
                        null);
                server.addFreshEntity(bot);
            }
            server.setBlockAndUpdate(pos, ModBlocks.BRICK_JUNGLE_CRACKED.get().defaultBlockState());
        }
        return InteractionResult.PASS;
    }
}

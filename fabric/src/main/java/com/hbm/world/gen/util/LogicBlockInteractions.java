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

package com.hbm.world.gen.util;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blockentity.encounter.LogicBlockEntity;
import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class LogicBlockInteractions {
    private LogicBlockInteractions() {}

    public static void interact(ServerLevel level, LogicBlockEntity logic, Player player) {
        switch (logic.interactionId) {
            case "POWER_LOCK" -> powerLock(level, logic.getBlockPos(), player);
            default ->
                    throw new IllegalStateException(
                            "Unknown logic interaction " + logic.interactionId);
        }
    }

    private static void powerLock(ServerLevel level, BlockPos pos, Player player) {
        EnergyHandler handler = null;
        for (Direction direction : Direction.VALUES) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof EnergyHandler energy) {
                handler = energy;
                break;
            }
        }

        MutableComponent prefix =
                Component.translatable("chat.logicBlock.powerLock")
                        .withStyle(ChatFormatting.LIGHT_PURPLE);
        if (handler == null || handler.getPower() <= 500_000L) {
            player.sendSystemMessage(
                    prefix.append(
                            Component.translatable("chat.logicBlock.chargeAdjacent")
                                    .withStyle(ChatFormatting.RESET)));
            return;
        }

        player.sendSystemMessage(
                prefix.append(
                        Component.translatable("chat.logicBlock.powerRestoredSafeUnlocked")
                                .withStyle(ChatFormatting.RESET)));
        for (Direction direction : Direction.VALUES) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof LockableBlockEntity safe) {
                safe.unlock();
                EntitySounds.play(
                        player,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.LOCK_OPEN.get(),
                        SoundSource.BLOCKS,
                        3.0F,
                        0.8F);
                break;
            }
        }
    }
}

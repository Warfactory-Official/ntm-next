// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.util;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blockentity.BlockEntityLogicBlock;
import com.hbm.blockentity.machine.BlockEntityLockableBase;
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

    public static void interact(ServerLevel level, BlockEntityLogicBlock logic, Player player) {
        switch (logic.interactionID) {
            case "POWER_LOCK" -> powerLock(level, logic.getBlockPos(), player);
            default ->
                    throw new IllegalStateException(
                            "Unknown logic interaction " + logic.interactionID);
        }
    }

    private static void powerLock(ServerLevel level, BlockPos pos, Player player) {
        IEnergyHandlerMK2 handler = null;
        for (Direction direction : Direction.VALUES) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof IEnergyHandlerMK2 energy) {
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
            if (neighbor instanceof BlockEntityLockableBase safe) {
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

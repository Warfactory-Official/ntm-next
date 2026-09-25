// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.IStepTickReceiver;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class BlockSpeedy extends Block implements IStepTickReceiver {

    public final double speed;

    public BlockSpeedy(double speed, Properties props) {
        super(props);
        this.speed = speed;
    }

    @Override
    public void onPlayerStep(Level level, BlockPos pos, Player player) {
        boost(level, player, speed);
    }

    static void boost(Level level, Player player, double speed) {
        if (!level.isClientSide()) return;

        if (player.zza != 0F || player.xxa != 0F) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * speed, motion.y, motion.z * speed);
        }
    }

    public Component tooltip() {
        return Component.translatable(
                        "desc.block.speedy.increasesSpeed", Mth.floor((speed - 1) * 100))
                .withStyle(ChatFormatting.BLUE);
    }
}

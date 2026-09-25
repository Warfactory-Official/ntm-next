// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ItemDesignatorRange extends ItemDesignator {

    public ItemDesignatorRange(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        HitResult hit = player.pick(300.0D, 1.0F, false);
        if (hit instanceof BlockHitResult bhr
                && bhr.getType() == HitResult.Type.BLOCK
                && !(level.getBlockState(bhr.getBlockPos()).getBlock() instanceof LaunchPad)) {
            if (!level.isClientSide()) {
                BlockPos pos = bhr.getBlockPos();
                player.getItemInHand(hand)
                        .set(
                                ModDataComponents.TARGET_DESIGNATOR.get(),
                                IDesignatorItem.pack(pos.getX(), pos.getZ()));
                level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.TECH_BLEEP.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
                player.sendSystemMessage(
                        Component.translatable(
                                "item.hbm.designator_range.set", pos.getX(), pos.getZ()));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

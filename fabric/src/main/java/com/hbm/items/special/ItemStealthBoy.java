// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.sound.ModSounds;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemStealthBoy extends Item {

    public ItemStealthBoy(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel server) {
            ItemStack stack = player.getItemInHand(hand);
            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.INVISIBILITY,
                            30 * SharedConstants.TICKS_PER_SECOND,
                            1,
                            true,
                            true));
            server.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.ITEM_UNPACK.get(),
                    SoundSource.PLAYERS,
                    1F,
                    1F);
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}

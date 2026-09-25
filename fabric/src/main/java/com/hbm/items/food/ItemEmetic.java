// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.items.special.ItemCustomLore;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemEmetic extends ItemCustomLore {

    private static final int HUNGER_TICKS = 50;
    private static final int HUNGER_AMPLIFIER = 49;

    public ItemEmetic(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, HUNGER_TICKS, HUNGER_AMPLIFIER));
        if (level instanceof ServerLevel server) {

            server.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.PLAYER_VOMIT.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
        return rest;
    }
}

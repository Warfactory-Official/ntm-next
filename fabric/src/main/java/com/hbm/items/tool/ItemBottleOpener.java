// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.special.ItemCustomLore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ItemBottleOpener extends ItemCustomLore {

    public ItemBottleOpener(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        RandomSource rand = mob.getRandom();
        int i = rand.nextInt(7);
        if (i == 0) mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 60 * 20, 0));
        if (i == 1) mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5 * 60 * 20, 2));
        if (i == 2) mob.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 5 * 60 * 20, 2));
        if (i == 3) mob.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60 * 20, 0));
        level.playSound(
                null, mob.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0F, 1.0F);
    }
}

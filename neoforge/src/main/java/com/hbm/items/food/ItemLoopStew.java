// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.items.special.ItemCustomLore;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemLoopStew extends ItemCustomLore {

    public ItemLoopStew(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60 * 20, 2));
        entity.addEffect(new MobEffectInstance(MobEffects.SPEED, 60 * 20, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 20, 2));
        return rest;
    }
}

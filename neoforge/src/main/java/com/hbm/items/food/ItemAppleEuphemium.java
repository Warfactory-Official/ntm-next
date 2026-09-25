// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemAppleEuphemium extends Item {

    public ItemAppleEuphemium(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide()) return rest;

        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Integer.MAX_VALUE, 120));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, Integer.MAX_VALUE, 120));
        return rest;
    }
}

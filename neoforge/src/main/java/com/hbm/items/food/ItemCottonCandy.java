// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemCottonCandy extends Item {

    public ItemCottonCandy(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide()) return rest;

        entity.addEffect(new MobEffectInstance(MobEffects.POISON, 15 * 20, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 5 * 20, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25 * 20, 2));
        entity.addEffect(new MobEffectInstance(MobEffects.SPEED, 25 * 20, 2));
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30 * 20, 4));
        return rest;
    }
}

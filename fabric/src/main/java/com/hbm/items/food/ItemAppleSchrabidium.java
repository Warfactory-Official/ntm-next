// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemAppleSchrabidium extends ItemAppleBase {

    public static final int FOREVER = Integer.MAX_VALUE;

    public ItemAppleSchrabidium(Properties props, Tier tier) {
        super(props, tier);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide()) return rest;

        if (tier == Tier.NUGGET) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 4));
            entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
        } else if (tier == Tier.INGOT) {
            grant(entity, 1200, 4, 4, 0, 4, 2, 2, 4, 9, 4, 9);
        } else {
            grant(entity, FOREVER, 4, 1, 0, 9, 4, 3, 4, 24, 14, 99);
        }
        return rest;
    }

    private static void grant(
            LivingEntity entity,
            int ticks,
            int regen,
            int resist,
            int fire,
            int damage,
            int dig,
            int speed,
            int jump,
            int health,
            int absorb,
            int saturation) {
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, regen));
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, resist));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, fire));
        entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, damage));
        entity.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, dig));
        entity.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, speed));
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks, jump));
        entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, ticks, health));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, absorb));
        entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, ticks, saturation));
    }
}

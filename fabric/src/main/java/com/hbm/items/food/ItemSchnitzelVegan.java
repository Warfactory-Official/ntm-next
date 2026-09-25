// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ItemSchnitzelVegan extends Item {

    private static final int FIRE_SECONDS = 5;

    private static final double LAUNCH = 2.0D;

    public ItemSchnitzelVegan(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide()) return rest;

        entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10 * 20, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 30 * 20, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 3 * 60 * 20, 4));
        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 3 * 20, 0));

        entity.igniteForSeconds(FIRE_SECONDS);
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, LAUNCH, motion.z);
        entity.hurtMarked = true;
        return rest;
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.lib.ModDamageTypes;
import com.hbm.potion.HbmPotion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemAppleLead extends ItemAppleBase {

    private static final float LETHAL = 500F;

    public ItemAppleLead(Properties props, Tier tier) {
        super(props, tier);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide()) return rest;

        if (tier == Tier.NUGGET) {
            entity.addEffect(new MobEffectInstance(HbmPotion.lead(), 15 * 20, 2));
        } else if (tier == Tier.INGOT) {
            entity.addEffect(new MobEffectInstance(HbmPotion.lead(), 60 * 20, 4));
        } else if (level instanceof ServerLevel server) {
            entity.hurtServer(server, server.damageSources().source(ModDamageTypes.LEAD), LETHAL);
        }
        return rest;
    }
}

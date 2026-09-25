// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemRBMKRod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardModifierRBMKHot implements IHazardModifier {

    @Override
    public double modify(ItemStack stack, @Nullable LivingEntity entity, double level) {
        if (!(stack.getItem() instanceof ItemRBMKRod)) return 0D;
        double heat = ItemRBMKRod.getHullHeat(stack);
        return Math.min(Math.ceil((heat - 100D) / 10D), 60D);
    }
}

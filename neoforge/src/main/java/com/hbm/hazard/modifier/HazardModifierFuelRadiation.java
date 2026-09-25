// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.modifier;

import java.util.function.ToDoubleFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardModifierFuelRadiation implements IHazardModifier {

    private final double target;
    private final ToDoubleFunction<ItemStack> depletion;

    public HazardModifierFuelRadiation(double target) {
        this(target, HazardModifierFuelRadiation::damageDepletion);
    }

    public HazardModifierFuelRadiation(double target, ToDoubleFunction<ItemStack> depletion) {
        this.target = target;
        this.depletion = depletion;
    }

    private static double damageDepletion(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max <= 0 ? 0D : (double) stack.getDamageValue() / max;
    }

    @Override
    public double modify(ItemStack stack, @Nullable LivingEntity entity, double level) {
        double spent = depletion.applyAsDouble(stack);
        if (spent <= 0D) return level;
        return level + (target - level) * Math.pow(Math.min(1D, spent), 0.4D);
    }
}

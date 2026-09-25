// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemRTGPellet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardModifierRTGRadiation implements IHazardModifier {

    private final double target;

    public HazardModifierRTGRadiation(double target) {
        this.target = target;
    }

    @Override
    public double modify(ItemStack stack, @Nullable LivingEntity entity, double level) {
        if (!(stack.getItem() instanceof ItemRTGPellet pellet)) return level;
        long max = pellet.getMaxLifespan();
        if (max <= 0L) return level;
        double depletion = 1D - ((double) pellet.getLifespan(stack) / (double) max);
        return level + (target - level) * depletion;
    }
}

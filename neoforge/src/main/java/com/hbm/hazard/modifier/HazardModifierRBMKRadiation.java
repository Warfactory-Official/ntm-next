// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.modifier;

import com.hbm.hazard.HazardRegistry;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardModifierRBMKRadiation implements IHazardModifier {

    private final double target;
    private final boolean linear;

    public HazardModifierRBMKRadiation(double target, boolean linear) {
        this.target = target;
        this.linear = linear;
    }

    @Override
    public double modify(ItemStack stack, @Nullable LivingEntity entity, double level) {
        if (stack.getItem() instanceof ItemRBMKRod) {
            double enrichment = ItemRBMKRod.getEnrichment(stack);
            double depletion = linear ? 1D - enrichment : 1D - Math.pow(enrichment, 2D);
            level = level + (target - level) * depletion;
            level += HazardRegistry.xe135 * ItemRBMKRod.getPoisonLevel(stack);
        } else if (stack.getItem() instanceof ItemRBMKPellet) {
            ItemRBMKPellet.Stage stage = ItemRBMKPellet.stage(stack);
            level = level + (target - level) * (stage.depletion() / 4D);
            if (stage.xenon()) level += HazardRegistry.xe135 * HazardRegistry.mult_nugget;
        }
        return level;
    }
}

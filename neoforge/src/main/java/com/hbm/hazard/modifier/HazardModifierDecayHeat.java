// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemDepletedFuel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record HazardModifierDecayHeat(double cooled, double hot) implements IHazardModifier {

    @Override
    public double modify(ItemStack stack, @Nullable LivingEntity entity, double level) {
        return ItemDepletedFuel.isHot(stack) ? hot : cooled;
    }
}

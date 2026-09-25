// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.modifier;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface IHazardModifier {

    static double evalAllModifiers(
            ItemStack stack,
            @Nullable LivingEntity entity,
            double level,
            List<IHazardModifier> mods) {
        if (mods.isEmpty()) return level;
        double result = level;
        for (int i = 0; i < mods.size(); i++) result = mods.get(i).modify(stack, entity, result);
        return result;
    }

    double modify(ItemStack stack, @Nullable LivingEntity entity, double level);
}

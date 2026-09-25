// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.modifier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

public class HazardModifierLevel implements IHazardModifier {

    private final IntegerProperty property;
    private final double[] levels;

    public HazardModifierLevel(IntegerProperty property, double... levels) {
        this.property = property;
        this.levels = levels;
    }

    @Override
    public double modify(ItemStack stack, @Nullable LivingEntity entity, double level) {
        Integer value =
                stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                        .get(property);
        return levels[value == null ? 0 : value];
    }
}

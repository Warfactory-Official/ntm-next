// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.world.item.ItemStack;

public interface ComponentStatePredicate extends DataComponentPredicate {
    boolean claims(DataComponentType<?> component, ItemStack stack);
}

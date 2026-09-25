// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidStackNTM;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public interface IFluidContainerItem {

    FluidStackNTM EMPTY_CONTENT = new FluidStackNTM(Fluids.EMPTY, 0L, 0);

    int capacity(ItemStack stack);

    FluidStackNTM getContent(ItemStack stack);

    void setContent(ItemStack stack, FluidStackNTM content);

    default ItemStack emptyContainer(ItemStack stack) {
        ItemStack result = stack.copyWithCount(1);
        setContent(result, null);
        return result;
    }

    default ItemStack filledContainer(ItemStack stack, FluidStackNTM content) {
        ItemStack result = stack.copyWithCount(1);
        setContent(result, content);
        return result;
    }

    default int getFill(ItemStack stack) {
        return (int) getContent(stack).amount();
    }

    int fill(ItemStack stack, Fluid type, int amount, int pressure);

    int drain(ItemStack stack, Fluid type, int amount, int pressure);

    default boolean canStore(Fluid type) {
        return machineConvertible();
    }

    default boolean machineFillable() {
        return false;
    }

    default @Nullable Fluid firstFluidType(ItemStack stack) {
        return null;
    }

    default boolean machineConvertible() {
        return !machineFillable();
    }
}

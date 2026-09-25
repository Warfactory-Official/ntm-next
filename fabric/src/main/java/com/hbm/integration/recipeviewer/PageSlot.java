// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.inventory.fluid.FluidStackNTM;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public interface PageSlot {

    PageSlot background();

    PageSlot item(ItemStack stack);

    default PageSlot item(ItemLike item) {
        return item(new ItemStack(item));
    }

    PageSlot items(List<ItemStack> stacks);

    PageSlot fluid(Fluid fluid, long milliBuckets, int pressure);

    PageSlot fluid(Fluid fluid);

    default PageSlot fluid(Fluid fluid, long milliBuckets) {
        return fluid(fluid, milliBuckets, 0);
    }

    default PageSlot fluid(FluidStackNTM fluid) {
        return fluid(fluid.type(), fluid.amount(), fluid.pressure());
    }

    default PageSlot fluid(FluidStackNTM fluid, long milliBuckets) {
        return fluid(fluid.type(), milliBuckets, fluid.pressure());
    }

    PageSlot tooltip(Tooltip tooltip);

    @FunctionalInterface
    interface Tooltip {

        void append(@Nullable ItemStack shown, Consumer<Component> lines);
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ModItems;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class ItemFluidContainerInfinite extends Item {

    private final @Nullable Supplier<Fluid> fluid;
    private final int amount;
    private final int chance;

    public ItemFluidContainerInfinite(
            Properties props, @Nullable Supplier<Fluid> fluid, int amount) {
        this(props, fluid, amount, 1);
    }

    public ItemFluidContainerInfinite(
            Properties props, @Nullable Supplier<Fluid> fluid, int amount, int chance) {
        super(props);
        this.fluid = fluid;
        this.amount = amount;
        this.chance = chance;
    }

    public @Nullable Fluid fluid() {
        return fluid == null ? null : fluid.get();
    }

    public int amount() {
        return amount;
    }

    public int chance() {
        return chance;
    }

    public boolean allowPressure(int pressure) {
        return this == ModItems.FLUID_BARREL_INFINITE.get() || pressure == 0;
    }
}

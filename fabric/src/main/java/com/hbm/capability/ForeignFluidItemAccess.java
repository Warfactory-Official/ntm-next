/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.capability;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.machine.FluidContainerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class ForeignFluidItemAccess {

    public static final int PRESSURE = 0;

    private ForeignFluidItemAccess() {}

    public static List<Item> containerItems() {
        List<Item> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof FluidContainerItem) out.add(item);
        }
        return out;
    }

    public static Fluid heldFluid(ItemStack stack) {
        FluidStackNTM content = content(stack);
        if (content == null || content.pressure() != PRESSURE || content.amount() <= 0)
            return Fluids.EMPTY;
        return content.type();
    }

    public static long heldMb(ItemStack stack) {
        return heldFluid(stack) == Fluids.EMPTY ? 0L : content(stack).amount();
    }

    public static long insertableMb(ItemStack stack, Fluid fluid, long maxMb) {
        FluidContainerItem item = itemOf(stack);
        if (item == null || fluid == Fluids.EMPTY || maxMb <= 0) return 0L;
        if (item.machineConvertible() && !item.canStore(fluid)) return 0L;
        int want = (int) Math.min(maxMb, Integer.MAX_VALUE);
        return item.fill(stack.copyWithCount(1), fluid, want, PRESSURE);
    }

    public static long extractableMb(ItemStack stack, Fluid fluid, long maxMb) {
        FluidContainerItem item = itemOf(stack);
        if (item == null || fluid == Fluids.EMPTY || maxMb <= 0) return 0L;
        int want = (int) Math.min(maxMb, Integer.MAX_VALUE);
        return item.drain(stack.copyWithCount(1), fluid, want, PRESSURE);
    }

    public static long capacityMb(ItemStack stack, Fluid fluid) {
        FluidContainerItem item = itemOf(stack);
        if (item == null || fluid == Fluids.EMPTY) return 0L;
        Fluid held = heldFluid(stack);
        if (held != Fluids.EMPTY && held != fluid) return 0L;
        FluidStackNTM content = content(stack);
        if (held == Fluids.EMPTY && content != null && content.amount() > 0) return 0L;
        if (item.machineConvertible() && !item.canStore(fluid)) return 0L;
        return item.capacity(stack);
    }

    public static ItemStack withContent(ItemStack stack, Fluid fluid, long mb) {
        FluidContainerItem item = itemOf(stack);
        if (item == null) return stack;
        if (mb <= 0 || fluid == Fluids.EMPTY) return item.emptyContainer(stack);
        return item.filledContainer(stack, new FluidStackNTM(fluid, mb, PRESSURE));
    }

    private static FluidContainerItem itemOf(ItemStack stack) {
        return stack.getItem() instanceof FluidContainerItem item ? item : null;
    }

    private static FluidStackNTM content(ItemStack stack) {
        FluidContainerItem item = itemOf(stack);
        return item == null ? null : item.getContent(stack);
    }
}

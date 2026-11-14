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

package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidStackNTM;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public interface FluidContainerItem {

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

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

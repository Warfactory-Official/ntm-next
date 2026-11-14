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
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidIconItem extends Item {

    public FluidIconItem(Properties properties) {
        super(properties);
    }

    public static ItemStack make(Fluid fluid) {
        return make(new FluidStackNTM(fluid, 0L));
    }

    public static ItemStack make(FluidStackNTM content) {
        ItemStack stack = new ItemStack(ModItems.FLUID_ICON);
        stack.applyComponents(patch(content));
        return stack;
    }

    public static DataComponentPatch patch(FluidStackNTM content) {
        DataComponentPatch.Builder patch =
                DataComponentPatch.builder().set(ModDataComponents.FLUID_CONTENT.get(), content);
        NTMFluidProperty prop = NTMFluidProperties.get(content.type());
        if (prop != null) {
            patch.set(
                    DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(
                            List.of(), List.of(), List.of(), List.of(prop.colorARGB())));
        }
        return patch.build();
    }

    public static Fluid getFluid(ItemStack stack) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        return content != null ? content.type() : Fluids.EMPTY;
    }

    @Override
    public Component getName(ItemStack stack) {
        Fluid fluid = getFluid(stack);
        if (fluid == Fluids.EMPTY) return super.getName(stack);
        return NTMFluidProperties.getDisplayName(fluid);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        if (content != null) {
            if (content.amount() > 0) adder.accept(Component.literal(content.amount() + "mB"));
            if (content.pressure() > 0) FluidTraitTooltip.pressure(content.pressure(), adder);
        }
        FluidTraitTooltip.describe(getFluid(stack), adder);
    }
}

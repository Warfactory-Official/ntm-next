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

package com.hbm.blocks.network;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidPipeBlockItem extends BlockItem {

    public FluidPipeBlockItem(Block block, Properties props) {
        super(block, props);
    }

    public ItemStack of(Fluid fluid) {
        ItemStack stack = new ItemStack(this);
        stack.set(ModDataComponents.FLUID_CONTENT.get(), new FluidStackNTM(fluid, 0, 0));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Fluid fluid = FluidPipeBlock.stampedFluid(stack);
        if (fluid == Fluids.EMPTY) return super.getName(stack);
        return Component.translatable("item.hbm.fluid_duct")
                .append(" ")
                .append(NTMFluidProperties.getDisplayName(fluid));
    }
}

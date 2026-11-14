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

package com.hbm.fabric.client;

import com.hbm.client.qmaw.QMAWClient;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRenderHandler;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class FluidTraitRenderHandler implements FluidVariantRenderHandler {

    private static final FluidTraitRenderHandler INSTANCE = new FluidTraitRenderHandler();

    private FluidTraitRenderHandler() {}

    public static void register() {
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid != Fluids.EMPTY
                    && !NTMFluidProperties.isForeign(fluid)
                    && FluidVariantRendering.getHandler(fluid) == null) {
                FluidVariantRendering.register(fluid, INSTANCE);
            }
        }
    }

    @Override
    public void appendTooltip(
            FluidVariant fluidVariant, List<Component> tooltip, TooltipFlag tooltipFlag) {
        FluidTraitTooltip.describe(fluidVariant.getFluid(), tooltip::add);
        QMAWClient.fluidTooltip(fluidVariant.getFluid(), tooltip::add);
    }
}

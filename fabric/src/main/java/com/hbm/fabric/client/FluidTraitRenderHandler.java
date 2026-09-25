// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        FluidTraitTooltip.addInfo(fluidVariant.getFluid(), tooltip::add);
        QMAWClient.fluidTooltip(fluidVariant.getFluid(), tooltip::add);
    }
}

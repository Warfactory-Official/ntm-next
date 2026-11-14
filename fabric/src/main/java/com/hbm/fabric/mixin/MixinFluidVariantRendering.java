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

package com.hbm.fabric.mixin;

import com.hbm.client.qmaw.QMAWClient;
import com.hbm.fabric.client.FluidTraitRenderHandler;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRenderHandler;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FluidVariantRendering.class, remap = false)
abstract class MixinFluidVariantRendering {
    @WrapOperation(
            method =
                    "getTooltip(Lnet/fabricmc/fabric/api/transfer/v1/fluid/FluidVariant;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/fabricmc/fabric/api/transfer/v1/client/fluid/FluidVariantRenderHandler;appendTooltip(Lnet/fabricmc/fabric/api/transfer/v1/fluid/FluidVariant;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private static void hbm$traitsBehindForeignHandler(
            FluidVariantRenderHandler handler,
            FluidVariant fluidVariant,
            List<Component> tooltip,
            TooltipFlag tooltipFlag,
            Operation<Void> original) {
        original.call(handler, fluidVariant, tooltip, tooltipFlag);
        FluidVariantRenderHandler registered =
                FluidVariantRendering.getHandler(fluidVariant.getFluid());
        if (registered != null && !(registered instanceof FluidTraitRenderHandler)) {
            FluidTraitTooltip.describe(fluidVariant.getFluid(), tooltip::add);
            QMAWClient.fluidTooltip(fluidVariant.getFluid(), tooltip::add);
        }
    }
}

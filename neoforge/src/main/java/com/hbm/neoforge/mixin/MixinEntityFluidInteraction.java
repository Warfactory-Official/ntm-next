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

package com.hbm.neoforge.mixin;

import com.hbm.blocks.fluid.InertFluidType;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityFluidInteraction.class)
abstract class MixinEntityFluidInteraction {
    @Inject(
            method =
                    "getTrackerFor(Lnet/neoforged/neoforge/fluids/FluidType;)Lnet/minecraft/world/entity/EntityFluidInteraction$Tracker;",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$untracked(FluidType type, CallbackInfoReturnable<Object> cir) {
        if (type instanceof InertFluidType) cir.setReturnValue(null);
    }
}

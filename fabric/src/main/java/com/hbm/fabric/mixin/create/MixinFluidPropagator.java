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

package com.hbm.fabric.mixin.create;

import com.zurrtum.create.content.fluids.FluidPropagator;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidPropagator.class)
public abstract class MixinFluidPropagator {

    @Inject(method = "hasFluidCapability", at = @At("HEAD"), cancellable = true)
    private static void hbm$seeABlockEntitylessProvider(
            BlockGetter world, BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (world.getBlockEntity(pos) != null) return;
        if (world instanceof Level level
                && FluidHelper.hasFluidInventory(level, pos, null, null, side)) {
            cir.setReturnValue(true);
        }
    }
}

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

package com.hbm.mixin;

import com.hbm.blocks.fluid.FiniteFluidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClipContext.class)
public abstract class MixinClipContext {
    @Shadow @Final private ClipContext.Fluid fluid;

    @Inject(method = "getFluidShape", at = @At("HEAD"), cancellable = true)
    private void hbm$finiteFluidShape(
            FluidState state,
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (!state.isEmpty() || fluid == ClipContext.Fluid.NONE || fluid == ClipContext.Fluid.WATER)
            return;
        BlockState block = level.getBlockState(pos);
        if (block.getBlock() instanceof FiniteFluidBlock finite) {
            cir.setReturnValue(
                    finite.fluidPickShape(block, fluid == ClipContext.Fluid.SOURCE_ONLY));
        }
    }
}

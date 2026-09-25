// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.fluid.BlockFluidFiniteBase;
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
        if (block.getBlock() instanceof BlockFluidFiniteBase finite) {
            cir.setReturnValue(
                    finite.fluidPickShape(block, fluid == ClipContext.Fluid.SOURCE_ONLY));
        }
    }
}

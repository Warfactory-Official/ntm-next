// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin.create;

import com.zurrtum.create.content.fluids.FluidPropagator;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
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

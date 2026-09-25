// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin;

import com.hbm.blocks.fluid.InertFluidType;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityFluidInteraction.class)
abstract class EntityFluidInteractionMixin {

    @Inject(
            method =
                    "getTrackerFor(Lnet/neoforged/neoforge/fluids/FluidType;)Lnet/minecraft/world/entity/EntityFluidInteraction$Tracker;",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$untracked(FluidType type, CallbackInfoReturnable<Object> cir) {
        if (type instanceof InertFluidType) cir.setReturnValue(null);
    }
}

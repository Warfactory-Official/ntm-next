// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.world.ImpactAtmosphere;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnvironmentAttributeSystem.class)
public abstract class MixinEnvironmentAttributeSystem {

    @Inject(
            method =
                    "addDefaultLayers(Lnet/minecraft/world/attribute/EnvironmentAttributeSystem$Builder;Lnet/minecraft/world/level/Level;)V",
            at = @At("TAIL"))
    private static void hbm$impactAtmosphere(
            EnvironmentAttributeSystem.Builder builder, Level level, CallbackInfo ci) {
        ImpactAtmosphere.addLayers(builder, level);
    }
}

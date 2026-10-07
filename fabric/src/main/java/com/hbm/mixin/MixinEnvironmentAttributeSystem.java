// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.environment.AtmosphereLayers;
import com.hbm.world.ImpactAtmosphere;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnvironmentAttributeSystem.class)
public abstract class MixinEnvironmentAttributeSystem {

    private static final String ADD_DEFAULT_LAYERS =
            "addDefaultLayers(Lnet/minecraft/world/attribute/EnvironmentAttributeSystem$Builder;Lnet/minecraft/world/level/Level;)V";

    @Inject(
            method = ADD_DEFAULT_LAYERS,
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/Level;canHaveWeather()Z"))
    private static void hbm$addonAtmosphere(
            EnvironmentAttributeSystem.Builder builder, Level level, CallbackInfo ci) {
        AtmosphereLayers.addLayers(builder, level);
    }

    @Inject(method = ADD_DEFAULT_LAYERS, at = @At("TAIL"))
    private static void hbm$impactAtmosphere(
            EnvironmentAttributeSystem.Builder builder, Level level, CallbackInfo ci) {
        if (!AtmosphereLayers.claimed(level)) ImpactAtmosphere.addLayers(builder, level);
    }
}

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

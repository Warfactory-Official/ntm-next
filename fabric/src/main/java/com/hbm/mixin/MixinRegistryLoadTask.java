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

import com.hbm.world.feature.Stratum3DFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.RegistryValidator;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RegistryLoadTask.class)
public abstract class MixinRegistryLoadTask {
    @WrapOperation(
            method = "validateRegistry",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/resources/RegistryValidator;validate(Lnet/minecraft/core/Registry;Ljava/util/Map;)V"))
    private <T> void hbm$validateStratumDomains(
            RegistryValidator<T> validator,
            Registry<T> registry,
            Map<ResourceKey<?>, Exception> errors,
            Operation<Void> original) {
        original.call(validator, registry, errors);
        if (registry.key().equals(Registries.FEATURE))
            Stratum3DFeature.validateRegistry(registry, errors);
    }
}

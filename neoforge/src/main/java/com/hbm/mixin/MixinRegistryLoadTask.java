// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.world.feature.Stratum3DConfig;
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
        if (registry.key().equals(Registries.CONFIGURED_FEATURE))
            Stratum3DConfig.validateRegistry(registry, errors);
    }
}

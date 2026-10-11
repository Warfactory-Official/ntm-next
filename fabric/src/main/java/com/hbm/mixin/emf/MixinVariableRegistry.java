// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.emf;

import com.hbm.items.armor.ArmorFlight;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import traben.entity_model_features.models.animation.math.variables.VariableRegistry;

@Mixin(value = VariableRegistry.class, remap = false)
public abstract class MixinVariableRegistry {
    @WrapOperation(
            method = "*",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z",
                            remap = true))
    private static boolean hbm$armorGliding(LivingEntity entity, Operation<Boolean> original) {
        return original.call(entity)
                || entity instanceof Player player && ArmorFlight.renderProne(player);
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin;

import com.hbm.interfaces.NtmDamageContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class LivingEntityEarlyDamageMixin {

    @ModifyExpressionValue(
            method = "hurtServer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/common/CommonHooks;onEntityIncomingDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)Z"))
    private boolean hbm$earlyCancellation(boolean canceled) {
        return canceled
                && (!((NtmDamageContext) this).hbm$ignoreEarlyCancellation()
                        || ((LivingEntity) (Object) this).isDeadOrDying());
    }
}

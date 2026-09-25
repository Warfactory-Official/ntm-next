// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.flywheel;

import com.hbm.client.VanishedEntities;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.engine_room.vanillin.visuals.LivingEntityVisual$Config", remap = false)
public class MixinLivingEntityVisualConfig {

    @Inject(method = "vanillaHandles", at = @At("HEAD"), cancellable = true)
    private void hbm$vanished(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (VanishedEntities.isVanished(entity)) cir.setReturnValue(true);
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class MixinCamera {

    @Inject(method = "calculateHudFov", at = @At("RETURN"), cancellable = true)
    private void hbm$gunModelFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ItemRenderWeaponBase.adjustHudFov(cir.getReturnValueF()));
    }

    @ModifyExpressionValue(
            method = "tickFov",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/AbstractClientPlayer;getFieldOfViewModifier(ZF)F"))
    private float hbm$gunViewFov(float modifier) {
        return ItemRenderWeaponBase.adjustViewFov(modifier);
    }
}

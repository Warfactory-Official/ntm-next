// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.GunKeybindHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void hbm$gunSuppressAttack(CallbackInfoReturnable<Boolean> cir) {
        if (GunKeybindHandler.isGunHeld()
                && GunKeybindHandler.isGunKey(Minecraft.getInstance().options.keyAttack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void hbm$gunSuppressContinueAttack(boolean down, CallbackInfo ci) {
        if (GunKeybindHandler.isGunHeld()
                && GunKeybindHandler.isGunKey(Minecraft.getInstance().options.keyAttack)) {
            ci.cancel();
        }
    }

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void hbm$gunSuppressPick(CallbackInfo ci) {
        if (GunKeybindHandler.isGunHeld()
                && GunKeybindHandler.isGunKey(Minecraft.getInstance().options.keyPickItem)) {
            ci.cancel();
        }
    }
}

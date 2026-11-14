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

import com.hbm.client.Keybinds;
import com.hbm.inventory.fluid.NTMFluidProperties;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("TAIL"))
    private void hbm$releaseFluidProperties(CallbackInfo ci) {
        NTMFluidProperties.onClientDisconnect();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void hbm$gunSuppressAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Keybinds.isGunHeld() && Keybinds.isGunKey(Minecraft.getInstance().options.keyAttack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void hbm$gunSuppressContinueAttack(boolean down, CallbackInfo ci) {
        if (Keybinds.isGunHeld() && Keybinds.isGunKey(Minecraft.getInstance().options.keyAttack)) {
            ci.cancel();
        }
    }

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void hbm$gunSuppressPick(CallbackInfo ci) {
        if (Keybinds.isGunHeld()
                && Keybinds.isGunKey(Minecraft.getInstance().options.keyPickItem)) {
            ci.cancel();
        }
    }
}

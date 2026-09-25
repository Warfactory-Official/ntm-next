// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.qmaw.QMAWClient;
import com.hbm.wiaj.CanneryClient;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void hbm$beginManualHover(CallbackInfo ci) {
        QMAWClient.beginFrame();
        CanneryClient.beginFrame();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void hbm$endManualHover(CallbackInfo ci) {
        QMAWClient.endFrame();
        CanneryClient.endFrame();
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.qmaw.QMAWClient;
import com.hbm.wiaj.CanneryClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {

    @WrapOperation(
            method = "onButton",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    private boolean hbm$manualMouse(
            Screen screen,
            MouseButtonEvent event,
            boolean doubleClick,
            Operation<Boolean> original) {
        return CanneryClient.mouse(screen, event)
                || QMAWClient.mouse(screen, event)
                || original.call(screen, event, doubleClick);
    }
}

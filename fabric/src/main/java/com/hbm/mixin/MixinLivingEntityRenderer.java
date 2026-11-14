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

import com.hbm.client.render.RiderTiltRenderState;
import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.render.util.RiderTilt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {
    @Inject(
            method =
                    "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$hideStealthPlayer(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar
                && (((com.hbm.interfaces.injected.PlayerAppearance) (avatar)).hbm$appearance()
                                & PlayerAppearance.STEALTH)
                        != 0) ci.cancel();
    }

    @Inject(
            method =
                    "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                            ordinal = 0,
                            shift = At.Shift.AFTER))
    private void hbm$tiltRider(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        if (!(state instanceof RiderTiltRenderState rider) || rider.hbm$riderTilt() == 0F) return;
        poseStack.translate(0F, RiderTilt.PIVOT, 0F);
        poseStack.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(rider.hbm$riderTilt()));
        poseStack.translate(0F, -RiderTilt.PIVOT, 0F);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void hbm$replaceManlySkin(
            LivingEntityRenderState state,
            boolean bodyVisible,
            boolean forceTransparent,
            boolean glowing,
            CallbackInfoReturnable<RenderType> cir) {
        if (!(state instanceof AvatarRenderState avatar)) return;
        if (avatar.isSpectator) return;
        if ((((com.hbm.interfaces.injected.PlayerAppearance) (avatar)).hbm$appearance()
                        & PlayerAppearance.MANLY)
                != 0) cir.setReturnValue(null);
    }
}

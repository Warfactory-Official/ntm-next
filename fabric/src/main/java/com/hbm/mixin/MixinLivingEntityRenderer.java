// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
                && (avatar.hbm$appearance() & PlayerAppearance.STEALTH) != 0) ci.cancel();
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
        poseStack.mulPose(Axis.ZP.rotationDegrees(rider.hbm$riderTilt()));
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
        if ((avatar.hbm$appearance() & PlayerAppearance.MANLY) != 0) cir.setReturnValue(null);
    }
}

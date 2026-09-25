// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.NTMSkybox;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class MixinSkyRenderer {
    @Shadow
    protected abstract void renderStars(float starBrightness, PoseStack poseStack);

    @Inject(method = "close", at = @At("HEAD"))
    private void hbm$closeSkyGeometry(CallbackInfo ci) {
        NTMSkybox.close();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void hbm$ntmSkybox(
            ClientLevel level,
            float partialTicks,
            Camera camera,
            SkyRenderState state,
            CallbackInfo ci) {
        NTMSkybox.extract(level, state);
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void hbm$impactStarsFirst(
            PoseStack poseStack,
            float sunAngle,
            float moonAngle,
            float starAngle,
            MoonPhase moonPhase,
            float rainBrightness,
            float starBrightness,
            CallbackInfo ci) {
        if (!NTMSkybox.impact() || starBrightness <= 0.0F) return;
        poseStack.pushPose();
        NTMSkybox.poseImpactStars(poseStack, starAngle);
        renderStars(starBrightness, poseStack);
        poseStack.popPose();
    }

    @WrapOperation(
            method = "renderSunMoonAndStars",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/SkyRenderer;renderStars(FLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void hbm$impactStarsReplaceVanilla(
            SkyRenderer sky, float starBrightness, PoseStack poseStack, Operation<Void> original) {
        if (!NTMSkybox.impact()) original.call(sky, starBrightness, poseStack);
    }

    @ModifyExpressionValue(
            method = "renderStars",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/renderer/RenderPipelines;STARS:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"))
    private RenderPipeline hbm$impactStarBlend(RenderPipeline original) {
        return NTMSkybox.impact() ? NTMSkybox.IMPACT_STARS : original;
    }

    @ModifyArg(
            method = "renderStars",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),
            index = 1)
    private Vector4f hbm$impactStarAlpha(Vector4f color) {
        if (NTMSkybox.impact()) color.w *= NTMSkybox.starAlpha();
        return color;
    }
}

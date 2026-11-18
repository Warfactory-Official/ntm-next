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

import com.hbm.api.client.CustomSkies;
import com.hbm.api.client.ShaderPacks;
import com.hbm.client.render.NTMSkybox;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
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
    protected abstract void renderStars(RenderPass pass, float starBrightness, PoseStack poseStack);

    @Inject(method = "close", at = @At("HEAD"))
    private void hbm$closeSkyGeometry(CallbackInfo ci) {
        NTMSkybox.close();
        CustomSkies.close();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void hbm$ntmSkybox(
            ClientLevel level,
            float partialTicks,
            Camera camera,
            SkyRenderState state,
            CallbackInfo ci) {
        if (CustomSkies.extract(level, partialTicks, camera, state)) NTMSkybox.clear();
        else NTMSkybox.extract(level, state);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hbm$customSky(GpuBufferSlice skyFog, SkyRenderState state, CallbackInfo ci) {
        RenderSystem.setShaderFog(skyFog);
        if (CustomSkies.render((SkyRenderer) (Object) this, state)) ci.cancel();
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void hbm$impactStarsFirst(
            RenderPass pass,
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
        renderStars(pass, starBrightness, poseStack);
        poseStack.popPose();
    }

    @WrapOperation(
            method = "renderSunMoonAndStars",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/SkyRenderer;renderStars(Lcom/mojang/renderpearl/api/commands/RenderPass;FLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void hbm$impactStarsReplaceVanilla(
            SkyRenderer sky,
            RenderPass pass,
            float starBrightness,
            PoseStack poseStack,
            Operation<Void> original) {
        if (!NTMSkybox.impact()) original.call(sky, pass, starBrightness, poseStack);
    }

    @ModifyExpressionValue(
            method = "renderStars",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/renderer/RenderPipelines;STARS:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline hbm$impactStarBlend(RenderPipeline original) {
        return NTMSkybox.impact() && !ShaderPacks.inUse() ? NTMSkybox.IMPACT_STARS : original;
    }

    @ModifyArg(
            method = "renderStars",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"),
            index = 1)
    private Vector4f hbm$impactStarAlpha(Vector4f color) {
        if (NTMSkybox.impact()) color.w *= NTMSkybox.starAlpha();
        return color;
    }
}

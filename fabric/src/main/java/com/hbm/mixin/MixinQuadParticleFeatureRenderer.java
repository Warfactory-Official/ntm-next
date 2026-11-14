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

import com.hbm.client.particle.ParticleLayers;
import com.hbm.client.render.RibbonPass;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.oit.OitStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QuadParticleFeatureRenderer.class)
abstract class MixinQuadParticleFeatureRenderer {
    @WrapOperation(
            method = "executeGroup",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/feature/QuadParticleFeatureRenderer;drawLayers(Lnet/minecraft/client/renderer/StagedVertexBuffer;Lnet/minecraft/client/renderer/feature/QuadParticleFeatureRenderer$PreparedGroup;Lcom/mojang/renderpearl/api/commands/RenderPass;Lnet/minecraft/client/renderer/oit/OitStage;)V"))
    private void hbm$particleLayerDepth(
            StagedVertexBuffer staged,
            QuadParticleFeatureRenderer.PreparedGroup group,
            RenderPass pass,
            OitStage stage,
            Operation<Void> original) {
        original.call(staged, group, pass, stage);
        if (stage != null || !RibbonPass.hasShaderPack() || RibbonPass.isShadow()) return;
        StagedVertexBuffer.Draw separate = group.layers().get(ParticleLayers.TRANSLUCENT_SEPARATE);
        StagedVertexBuffer.Draw translucent = group.layers().get(ParticleLayers.TRANSLUCENT);
        StagedVertexBuffer.ExecuteInfo separateInfo =
                separate == null ? null : staged.getExecuteInfo(separate);
        StagedVertexBuffer.ExecuteInfo translucentInfo =
                translucent == null ? null : staged.getExecuteInfo(translucent);
        if (separateInfo == null && translucentInfo == null) return;
        var texture =
                group.textures()
                        .get(
                                separateInfo != null
                                        ? ParticleLayers.TRANSLUCENT_SEPARATE
                                        : ParticleLayers.TRANSLUCENT);
        pass.setPipeline(
                RenderSystem.getCompiledPipeline(ParticleLayers.TRANSLUCENT_DEPTH_PIPELINE));
        pass.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());
        if (separateInfo != null) hbm$writeLayerDepth(pass, separateInfo);
        if (translucentInfo != null) hbm$writeLayerDepth(pass, translucentInfo);
    }

    @Unique
    private static void hbm$writeLayerDepth(RenderPass pass, StagedVertexBuffer.ExecuteInfo info) {
        pass.setVertexBuffer(0, info.vertexBuffer().slice());
        pass.setIndexBuffer(info.indexBuffer(), info.indexType());
        pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
    }
}

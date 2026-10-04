// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.particle.ParticleLayers;
import com.hbm.client.render.RibbonPass;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QuadParticleFeatureRenderer.class)
abstract class MixinQuadParticleFeatureRenderer {
    @WrapOperation(
            method = "drawLayers",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/particle/SingleQuadParticle$Layer;pipeline()Lcom/mojang/blaze3d/pipeline/RenderPipeline;"))
    private static RenderPipeline hbm$particleLayerAlpha(
            SingleQuadParticle.Layer layer, Operation<RenderPipeline> original) {
        if ((layer == ParticleLayers.TRANSLUCENT_SEPARATE || layer == ParticleLayers.TRANSLUCENT)
                && Minecraft.getInstance().levelRenderer.particlesTarget() != null)
            return ParticleLayers.TRANSLUCENT_COMPOSITE_PIPELINE;
        return original.call(layer);
    }

    @WrapOperation(
            method = "executeGroup",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/feature/QuadParticleFeatureRenderer;drawLayers(Lnet/minecraft/client/renderer/StagedVertexBuffer;Ljava/util/Map;Lcom/mojang/blaze3d/systems/RenderPass;Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
    private void hbm$particleLayerDepth(
            StagedVertexBuffer staged,
            Map<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> layers,
            RenderPass pass,
            TextureManager textures,
            Operation<Void> original) {
        original.call(staged, layers, pass, textures);
        if (Minecraft.getInstance().levelRenderer.particlesTarget() == null
                && (!RibbonPass.hasShaderPack() || RibbonPass.isShadow())) return;
        StagedVertexBuffer.Draw separate = layers.get(ParticleLayers.TRANSLUCENT_SEPARATE);
        StagedVertexBuffer.Draw translucent = layers.get(ParticleLayers.TRANSLUCENT);
        StagedVertexBuffer.ExecuteInfo separateInfo =
                separate == null ? null : staged.getExecuteInfo(separate);
        StagedVertexBuffer.ExecuteInfo translucentInfo =
                translucent == null ? null : staged.getExecuteInfo(translucent);
        if (separateInfo == null && translucentInfo == null) return;
        var texture = textures.getTexture(TextureAtlas.LOCATION_PARTICLES);
        pass.setPipeline(ParticleLayers.TRANSLUCENT_DEPTH_PIPELINE);
        pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
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

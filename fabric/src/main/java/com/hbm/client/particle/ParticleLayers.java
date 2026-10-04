// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.client.render.WorldRenderPipeline;
import com.hbm.lib.Library;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import java.util.Optional;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;

public final class ParticleLayers {
    private static final float EFFECT_RENDERER_ALPHA_CUTOUT = 1.5F / 255.0F;

    public static final RenderPipeline TRANSLUCENT_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                            .withLocation("pipeline/ntm_particle_translucent")
                            .withFragmentShader(ParticleRenderTypes.PARTICLE_CUTOUT)
                            .withShaderDefine("ALPHA_CUTOUT", EFFECT_RENDERER_ALPHA_CUTOUT)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA)))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderPipeline TRANSLUCENT_SEPARATE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                            .withLocation("pipeline/ntm_particle_translucent_separate")
                            .withShaderDefine("EMISSIVE")
                            .withFragmentShader(ParticleRenderTypes.PARTICLE_CUTOUT)
                            .withShaderDefine("ALPHA_CUTOUT", EFFECT_RENDERER_ALPHA_CUTOUT)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderPipeline TRANSLUCENT_COMPOSITE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                            .withLocation(Library.id("pipeline/particle_composite"))
                            .withFragmentShader(ParticleRenderTypes.PARTICLE_CUTOUT)
                            .withShaderDefine("ALPHA_CUTOUT", EFFECT_RENDERER_ALPHA_CUTOUT)
                            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderPipeline TRANSLUCENT_DEPTH_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                            .withLocation(Library.id("pipeline/particle_depth"))
                            .withFragmentShader(ParticleRenderTypes.PARTICLE_CUTOUT)
                            .withShaderDefine("ALPHA_CUTOUT", EFFECT_RENDERER_ALPHA_CUTOUT)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            Optional.of(BlendFunction.TRANSLUCENT),
                                            GpuFormat.RGBA8_UNORM,
                                            ColorTargetState.WRITE_NONE))
                            .withDepthStencilState(DepthStencilState.DEFAULT));

    public static final RenderPipeline ADDITIVE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                            .withLocation("pipeline/ntm_particle_additive")
                            .withShaderDefine("ADDITIVE_EMISSION")
                            .withShaderDefine("ALPHA_CUTOUT", 0F)
                            .withFragmentShader(ParticleRenderTypes.PARTICLE_FADE)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderPipeline ADDITIVE_NO_FOG_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(ParticleRenderTypes.PARTICLE_NO_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_particle_additive_no_fog")
                            .withShaderDefine("ADDITIVE_EMISSION")
                            .withShaderDefine("ALPHA_CUTOUT", 0F)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final SingleQuadParticle.Layer TRANSLUCENT =
            new SingleQuadParticle.Layer(
                    true, TextureAtlas.LOCATION_PARTICLES, TRANSLUCENT_PIPELINE);
    public static final SingleQuadParticle.Layer TRANSLUCENT_SEPARATE =
            new SingleQuadParticle.Layer(
                    true, TextureAtlas.LOCATION_PARTICLES, TRANSLUCENT_SEPARATE_PIPELINE);
    public static final SingleQuadParticle.Layer ADDITIVE =
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, ADDITIVE_PIPELINE);
    public static final SingleQuadParticle.Layer ADDITIVE_NO_FOG =
            new SingleQuadParticle.Layer(
                    true, TextureAtlas.LOCATION_PARTICLES, ADDITIVE_NO_FOG_PIPELINE);

    private ParticleLayers() {}
}

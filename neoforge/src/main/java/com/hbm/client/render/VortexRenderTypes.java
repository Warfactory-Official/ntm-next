// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.function.Function;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class VortexRenderTypes {

    public static final RenderPipeline TRANSLUCENT_PIPELINE =
            pipeline(
                    "pipeline/ntm_vortex_translucent",
                    new BlendFunction(
                            BlendFactor.SRC_ALPHA,
                            BlendFactor.ONE_MINUS_SRC_ALPHA,
                            BlendFactor.ONE,
                            BlendFactor.ZERO));
    public static final RenderPipeline ADDITIVE_PIPELINE =
            pipeline("pipeline/ntm_vortex_additive", BlendFunction.LIGHTNING);
    public static final RenderPipeline CUTOUT_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_vortex_cutout")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(false));

    private static final Function<Identifier, RenderType> TRANSLUCENT =
            Util.memoize(
                    texture ->
                            RenderType.create(
                                    "ntm_vortex_translucent",
                                    RenderSetup.builder(TRANSLUCENT_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .sortOnUpload()
                                            .createRenderSetup()));
    private static final Function<Identifier, RenderType> ADDITIVE =
            Util.memoize(
                    texture ->
                            RenderType.create(
                                    "ntm_vortex_additive",
                                    RenderSetup.builder(ADDITIVE_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .sortOnUpload()
                                            .createRenderSetup()));
    private static final Function<Identifier, RenderType> CUTOUT =
            Util.memoize(
                    texture ->
                            RenderType.create(
                                    "ntm_vortex_cutout",
                                    RenderSetup.builder(CUTOUT_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .createRenderSetup()));

    private VortexRenderTypes() {}

    private static RenderPipeline pipeline(String location, BlendFunction blend) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader(
                                blend == BlendFunction.LIGHTNING
                                        ? ParticleRenderTypes.ENTITY_FADE
                                        : Identifier.withDefaultNamespace("core/entity"))
                        .withShaderDefine("EMISSIVE")
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                        .withColorTargetState(new ColorTargetState(blend))
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(
                                new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                        .withCull(false));
    }

    public static RenderType translucent(Identifier texture) {
        return TRANSLUCENT.apply(texture);
    }

    public static RenderType additive(Identifier texture) {
        return ADDITIVE.apply(texture);
    }

    public static RenderType cutout(Identifier texture) {
        return CUTOUT.apply(texture);
    }
}

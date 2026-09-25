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
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class CloudRenderTypes {

    public static final RenderPipeline OPAQUE_PIPELINE =
            pipeline("pipeline/ntm_cloud_opaque", null, true, false);
    public static final RenderPipeline OPAQUE_UNCULLED_PIPELINE =
            pipeline("pipeline/ntm_cloud_opaque_unculled", null, false, false);
    public static final RenderPipeline TRANSLUCENT_PIPELINE =
            pipeline(
                    "pipeline/ntm_cloud_translucent",
                    new BlendFunction(
                            BlendFactor.SRC_ALPHA,
                            BlendFactor.ONE_MINUS_SRC_ALPHA,
                            BlendFactor.ONE,
                            BlendFactor.ZERO),
                    true,
                    false);
    public static final RenderPipeline ADDITIVE_PIPELINE =
            pipeline("pipeline/ntm_cloud_additive", BlendFunction.LIGHTNING, true, false);
    public static final RenderPipeline ADDITIVE_LIT_UNCULLED_PIPELINE =
            pipeline(
                    "pipeline/ntm_cloud_additive_lit_unculled",
                    BlendFunction.LIGHTNING,
                    false,
                    true);
    private static final Identifier WHITE = RenderTextures.WHITE;
    public static final RenderType OPAQUE = create("ntm_cloud_opaque", OPAQUE_PIPELINE);
    public static final RenderType OPAQUE_UNCULLED =
            create("ntm_cloud_opaque_unculled", OPAQUE_UNCULLED_PIPELINE);
    public static final RenderType TRANSLUCENT =
            create("ntm_cloud_translucent", TRANSLUCENT_PIPELINE);
    public static final RenderType ADDITIVE = create("ntm_cloud_additive", ADDITIVE_PIPELINE);
    public static final RenderType ADDITIVE_LIT_UNCULLED =
            create("ntm_cloud_additive_lit_unculled", ADDITIVE_LIT_UNCULLED_PIPELINE);

    private CloudRenderTypes() {}

    private static RenderPipeline pipeline(
            String location, BlendFunction blend, boolean cull, boolean lit) {
        RenderPipeline.Builder builder =
                RenderPipeline.builder(
                        lit
                                ? RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET
                                : RenderPipelines.MATRICES_FOG_SNIPPET);
        if (!lit) builder.withShaderDefine("NO_CARDINAL_LIGHTING");
        return WorldRenderPipeline.of(
                builder.withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader(
                                blend == BlendFunction.LIGHTNING
                                        ? ParticleRenderTypes.ENTITY_FADE
                                        : Identifier.withDefaultNamespace("core/entity"))
                        .withShaderDefine("EMISSIVE")
                        .withShaderDefine("NO_OVERLAY")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                        .withColorTargetState(
                                blend == null
                                        ? ColorTargetState.DEFAULT
                                        : new ColorTargetState(blend))
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(DepthStencilState.DEFAULT)
                        .withCull(cull));
    }

    private static RenderType create(String name, RenderPipeline pipeline) {
        return RenderType.create(
                name,
                RenderSetup.builder(pipeline).withTexture("Sampler0", WHITE).createRenderSetup());
    }
}

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

public final class ArmorRenderTypes {

    public static final RenderPipeline LOOT_HELMET_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                            .withLocation("pipeline/ntm_loot_helmet")
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withShaderDefine("NO_OVERLAY")
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withCull(true));

    public static final RenderPipeline LOOT_GLOW_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_loot_glow")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withColorTargetState(ColorTargetState.DEFAULT)
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(true));

    public static final Function<Identifier, RenderType> LOOT_HELMET =
            Util.memoize(
                    texture ->
                            RenderType.create(
                                    "ntm_loot_helmet",
                                    RenderSetup.builder(LOOT_HELMET_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .useLightmap()
                                            .createRenderSetup()));
    public static final Function<Identifier, RenderType> LOOT_GLOW =
            Util.memoize(
                    texture ->
                            RenderType.create(
                                    "ntm_loot_glow",
                                    RenderSetup.builder(LOOT_GLOW_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .createRenderSetup()));

    public static final RenderPipeline LAMP_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_armor_lamp")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withColorTargetState(ColorTargetState.DEFAULT)
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(false));

    private static final Identifier WHITE = RenderTextures.WHITE;

    public static final RenderType LAMP =
            RenderType.create(
                    "ntm_armor_lamp",
                    RenderSetup.builder(LAMP_PIPELINE)
                            .withTexture("Sampler0", WHITE)
                            .createRenderSetup());

    private ArmorRenderTypes() {}
}

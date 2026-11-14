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

package com.hbm.client.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class BeamRenderTypes {

    public static final RenderPipeline ADDITIVE_PIPELINE =
            additive("pipeline/ntm_beam_additive", false);

    public static final RenderType ADDITIVE =
            RenderType.create(
                    "ntm_beam_additive",
                    RenderSetup.builder(ADDITIVE_PIPELINE).createRenderSetup());

    public static final RenderPipeline ADDITIVE_CULL_PIPELINE =
            additive("pipeline/ntm_beam_additive_cull", true);

    public static final RenderType ADDITIVE_CULL =
            RenderType.create(
                    "ntm_beam_additive_cull",
                    RenderSetup.builder(ADDITIVE_CULL_PIPELINE).createRenderSetup());

    public static final RenderPipeline ADDITIVE_SHADED_PIPELINE =
            lightmapped("pipeline/ntm_beam_additive_shaded", false, true);

    public static final RenderType ADDITIVE_SHADED =
            RenderType.create(
                    "ntm_beam_additive_shaded",
                    RenderSetup.builder(ADDITIVE_SHADED_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .createRenderSetup());

    public static final RenderPipeline ADDITIVE_NO_FOG_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(ParticleRenderTypes.MATRICES_NO_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_beam_additive_no_fog")
                            .withVertexShader(ParticleRenderTypes.LIGHTNING_NO_FOG)
                            .withFragmentShader(ParticleRenderTypes.LIGHTNING_NO_FOG)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false))
                            .withCull(false));

    public static final RenderType ADDITIVE_NO_FOG =
            RenderType.create(
                    "ntm_beam_additive_no_fog",
                    RenderSetup.builder(ADDITIVE_NO_FOG_PIPELINE).createRenderSetup());

    public static final RenderPipeline LINE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/ntm_beam_line")
                            .withColorTargetState(ColorTargetState.DEFAULT)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderType LINE =
            RenderType.create(
                    "ntm_beam_line", RenderSetup.builder(LINE_PIPELINE).createRenderSetup());

    private BeamRenderTypes() {}

    private static RenderPipeline additive(String location, boolean cull) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/rendertype_lightning")
                        .withFragmentShader("core/rendertype_lightning")
                        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(
                                new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                        .withCull(cull));
    }

    private static RenderPipeline lightmapped(String location, boolean writeDepth) {
        return lightmapped(location, writeDepth, false);
    }

    private static RenderPipeline lightmapped(String location, boolean writeDepth, boolean shaded) {
        Builder builder =
                RenderPipeline.builder(
                                shaded
                                        ? RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET
                                        : RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                        .withShaderDefine("NO_OVERLAY")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(
                                new DepthStencilState(
                                        DepthStencilState.DEFAULT.depthTest(), writeDepth))
                        .withCull(false);
        if (!shaded) builder.withShaderDefine("NO_CARDINAL_LIGHTING");
        return WorldRenderPipeline.of(builder);
    }
}

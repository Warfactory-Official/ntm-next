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

import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class TorexRenderTypes {

    public static final RenderPipeline FLARE_PIPELINE =
            billboard("pipeline/ntm_torex_flare", BlendFunction.LIGHTNING, false, false);
    public static final RenderPipeline FLASH_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(ParticleRenderTypes.MATRICES_NO_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_torex_flash")
                            .withVertexShader(ParticleRenderTypes.LIGHTNING_NO_FOG)
                            .withFragmentShader(ParticleRenderTypes.LIGHTNING_NO_FOG)
                            .withShaderDefine("ADDITIVE_EMISSION")
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));
    public static final RenderType FLASH =
            RenderType.create(
                    "ntm_torex_flash", RenderSetup.builder(FLASH_PIPELINE).createRenderSetup());
    private static final Identifier CLOUDLET_TEX =
            RegistryUtil.id("textures/particle/particle_base.png");
    private static final Identifier FLARE_TEX = RegistryUtil.id("textures/particle/flare.png");
    public static final RenderType FLARE =
            RenderType.create(
                    "ntm_torex_flare",
                    RenderSetup.builder(FLARE_PIPELINE)
                            .withTexture("Sampler0", FLARE_TEX)
                            .createRenderSetup());
    private static final BlendFunction CLOUDLET_BLEND =
            new BlendFunction(
                    BlendFactor.SRC_ALPHA,
                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                    BlendFactor.ONE,
                    BlendFactor.ZERO);
    public static final RenderPipeline CLOUDLET_PIPELINE =
            billboard("pipeline/ntm_torex_cloudlet", CLOUDLET_BLEND, false, true);
    public static final RenderType CLOUDLET =
            RenderType.create(
                    "ntm_torex_cloudlet",
                    RenderSetup.builder(CLOUDLET_PIPELINE)
                            .withTexture("Sampler0", CLOUDLET_TEX)
                            .createRenderSetup());

    private TorexRenderTypes() {}

    private static RenderPipeline billboard(
            String location, BlendFunction blend, boolean cull, boolean orderedDepth) {
        var builder = RenderPipeline.builder(ParticleRenderTypes.MATRICES_NO_FOG_SNIPPET);
        if (blend == BlendFunction.LIGHTNING) builder.withShaderDefine("ADDITIVE_EMISSION");
        builder.withLocation(location)
                .withVertexShader(ParticleRenderTypes.ENTITY_NO_FOG)
                .withFragmentShader(ParticleRenderTypes.ENTITY_NO_FOG)
                .withShaderDefine("NO_OVERLAY")
                .withShaderDefine("NO_CARDINAL_LIGHTING")
                .withShaderDefine("EMISSIVE")
                .withShaderDefine("DISCARD_ZERO_ALPHA")
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withColorTargetState(new ColorTargetState(blend))
                .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(
                        blend == BlendFunction.LIGHTNING
                                ? new DepthStencilState(
                                        DepthStencilState.DEFAULT.depthTest(), false)
                                : DepthStencilState.DEFAULT)
                .withCull(cull);
        return orderedDepth
                ? WorldRenderPipeline.orderedDepth(builder)
                : WorldRenderPipeline.of(builder);
    }
}

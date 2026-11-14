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

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.function.Function;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class BobbleRenderTypes {
    private static final float ALPHA_DISCARD_ONLY_ZERO = 0.5F / 255.0F;

    public static final RenderPipeline FIGURINE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                            .withLocation("pipeline/ntm_bobble_figurine")
                            .withShaderDefine("ALPHA_CUTOUT", ALPHA_DISCARD_ONLY_ZERO)
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withCull(false));

    public static final RenderPipeline GLOW_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
                            .withLocation("pipeline/ntm_bobble_glow")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("ALPHA_CUTOUT", ALPHA_DISCARD_ONLY_ZERO)
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(true));

    private static final Function<Identifier, RenderType> FIGURINE =
            Util.memoize(
                    texture ->
                            RenderType.create(
                                    "ntm_bobble_figurine",
                                    RenderSetup.builder(FIGURINE_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .useLightmap()
                                            .useOverlay()
                                            .affectsCrumbling()
                                            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                                            .createRenderSetup()));

    private static final Function<Identifier, RenderType> GLOW =
            Util.memoize(
                    texture ->
                            RenderType.create(
                                    "ntm_bobble_glow",
                                    RenderSetup.builder(GLOW_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .createRenderSetup()));

    private BobbleRenderTypes() {}

    public static RenderType figurine(Identifier texture) {
        return FIGURINE.apply(texture);
    }

    public static RenderType glow(Identifier texture) {
        return GLOW.apply(texture);
    }
}

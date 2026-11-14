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

import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

public final class HologramRenderTypes {

    public static final RenderPipeline GHOST_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                            .withLocation("pipeline/ntm_hologram_ghost")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false))
                            .withCull(true));
    public static final RenderType GHOST =
            RenderType.create(
                    "ntm_hologram_ghost",
                    RenderSetup.builder(GHOST_PIPELINE)
                            .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                            .useLightmap()
                            .useOverlay()
                            .createRenderSetup());

    private HologramRenderTypes() {}
}

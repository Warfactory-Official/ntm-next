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

public final class FlatCutout {

    static final RenderPipeline PIPELINE = pipeline("pipeline/ntm_flat_cutout", false);
    static final RenderPipeline CULL_PIPELINE = pipeline("pipeline/ntm_flat_cutout_cull", true);

    private static final Function<Identifier, RenderType> TYPES =
            types("ntm_flat_cutout", PIPELINE);
    private static final Function<Identifier, RenderType> CULLED =
            types("ntm_flat_cutout_cull", CULL_PIPELINE);

    private FlatCutout() {}

    public static RenderType of(Identifier texture) {
        return TYPES.apply(texture);
    }

    public static RenderType culled(Identifier texture) {
        return CULLED.apply(texture);
    }

    private static RenderPipeline pipeline(String location, boolean cull) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader("core/entity")
                        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(DepthStencilState.DEFAULT)
                        .withCull(cull));
    }

    private static Function<Identifier, RenderType> types(String name, RenderPipeline pipeline) {
        return Util.memoize(
                texture ->
                        RenderType.create(
                                name,
                                RenderSetup.builder(pipeline)
                                        .withTexture("Sampler0", texture)
                                        .useLightmap()
                                        .affectsCrumbling()
                                        .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                                        .createRenderSetup()));
    }
}

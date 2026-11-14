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
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;

public final class ArmorGlint {
    public static final OitPipelineSet OIT = createOit();
    public static final RenderType TYPE =
            RenderType.create(
                    "ntm_armor_glint",
                    RenderSetup.builder(RenderPipelines.GLINT)
                            .setOitPipelines(OIT)
                            .withTexture("Sampler0", ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR)
                            .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .withForcedSolidModelPhase()
                            .createRenderSetup());

    private ArmorGlint() {}

    private static OitPipelineSet createOit() {
        RenderPipeline source = RenderPipelines.GLINT;
        var snippet =
                new Snippet(
                        source.getShaders(),
                        Optional.of(source.getShaderDefines()),
                        Optional.of(source.getBindGroupLayouts()),
                        source.getColorTargetStates().toArray(ColorTargetState[]::new),
                        source.getColorTargetStates().size(),
                        Optional.of(source.getDepthStencilState()),
                        Optional.of(source.getPolygonMode()),
                        Optional.of(source.isCull()),
                        source.vertexFormatPerBuffer.toArray(VertexFormat[]::new),
                        Optional.of(source.getPrimitiveTopology()),
                        source.pushConstantSize());
        var pipeline =
                RenderPipeline.builder(snippet)
                        .withLocation(RegistryUtil.id("pipeline/armor_glint"))
                        .withFragmentShader(RegistryUtil.id("core/armor_glint"))
                        .build();
        return NativeOitPipelines.create(pipeline, false);
    }
}

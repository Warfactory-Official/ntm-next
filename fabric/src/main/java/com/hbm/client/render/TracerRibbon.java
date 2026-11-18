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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.oit.OitPipelineSet;

public final class TracerRibbon {
    public static final float MIN_WIDTH_PIXELS = 1F;
    public static final float FILTER_PADDING_PIXELS = 1F;
    public static final int STRIDE = 52;
    public static final VertexFormat FORMAT =
            VertexFormat.builder(0)
                    .addAttribute("Position", GpuFormat.RGB32_FLOAT)
                    .addAttribute("OtherPosition", GpuFormat.RGB32_FLOAT)
                    .addAttribute("Color", GpuFormat.RGBA8_UNORM)
                    .addAttribute("OtherColor", GpuFormat.RGBA8_UNORM)
                    .addAttribute("Widths", GpuFormat.RG32_FLOAT)
                    .addAttribute("UV0", GpuFormat.RG32_FLOAT)
                    .addAttribute("UV2", GpuFormat.RG16_SINT)
                    .build();

    private TracerRibbon() {}

    static RenderPipeline pipeline(Builder builder) {
        return new Pipeline(builder.build());
    }

    private static final class Pipeline extends RenderPipeline implements OitPipelineOwner {
        private final ShaderDefines zeroToOne;
        private final ShaderDefines negativeOneToOne;
        private final OitPipelineSet oitPipelines;

        private Pipeline(RenderPipeline source) {
            this(source, true);
        }

        private Pipeline(RenderPipeline source, boolean buildOit) {
            super(
                    source.getLocation(),
                    source.getShaders(),
                    source.getShaderDefines(),
                    source.getBindGroupLayouts(),
                    source.getColorTargetStates().toArray(ColorTargetState[]::new),
                    source.getDepthStencilState(),
                    source.getPolygonMode(),
                    source.isCull(),
                    source.vertexFormatPerBuffer.toArray(VertexFormat[]::new),
                    source.getPrimitiveTopology(),
                    source.pushConstantSize(),
                    source.getSortKey());
            zeroToOne =
                    source.getShaderDefines()
                            .withOverrides(
                                    ShaderDefines.builder()
                                            .define("NTM_RIBBON_DEPTH_ZERO_TO_ONE", 1)
                                            .build());
            negativeOneToOne =
                    source.getShaderDefines()
                            .withOverrides(
                                    ShaderDefines.builder()
                                            .define("NTM_RIBBON_DEPTH_ZERO_TO_ONE", 0)
                                            .build());
            var stages = buildOit ? NativeOitPipelines.create(source, false) : null;
            oitPipelines =
                    stages == null
                            ? null
                            : new OitPipelineSet(
                                    new Pipeline(stages.depthBoundsPipeline(), false),
                                    new Pipeline(stages.transmittancePipeline(), false),
                                    new Pipeline(stages.accumulatePipeline(), false));
        }

        @Override
        public OitPipelineSet oitPipelines() {
            return oitPipelines;
        }

        @Override
        public boolean orderedBlend() {
            return false;
        }

        @Override
        public ShaderDefines getShaderDefines() {
            return RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()
                    ? zeroToOne
                    : negativeOneToOne;
        }
    }
}

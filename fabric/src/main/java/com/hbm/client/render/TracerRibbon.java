// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderDefines;

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

    static RenderPipeline pipeline(RenderPipeline.Builder builder) {
        return new Pipeline(builder.build());
    }

    private static final class Pipeline extends RenderPipeline {
        private final ShaderDefines zeroToOne;
        private final ShaderDefines negativeOneToOne;

        private Pipeline(RenderPipeline source) {
            super(
                    source.getLocation(),
                    source.getVertexShader(),
                    source.getFragmentShader(),
                    source.getShaderDefines(),
                    source.getBindGroupLayouts(),
                    source.getColorTargetStates(),
                    source.getDepthStencilState(),
                    source.getPolygonMode(),
                    source.isCull(),
                    source.vertexFormatPerBuffer,
                    source.getPrimitiveTopology(),
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
        }

        @Override
        public ShaderDefines getShaderDefines() {

            return RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()
                    ? zeroToOne
                    : negativeOneToOne;
        }
    }
}

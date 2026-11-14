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
import com.mojang.renderpearl.api.pipeline.BlendOp;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.Optional;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import org.jspecify.annotations.Nullable;

public final class NativeOitPipelines {
    private NativeOitPipelines() {}

    public static @Nullable OitPipelineSet create(RenderPipeline source, boolean orderedDepth) {
        if (source.getColorTargetStates().isEmpty()) return null;
        ColorTargetState target = source.getColorTargetStates().getFirst();
        if (target.writeMask() == ColorTargetState.WRITE_NONE || target.blendFunction().isEmpty())
            return null;
        var equation = target.blendFunction().orElseThrow().color();
        BlendFactor from = equation.sourceFactor();
        BlendFactor to = equation.destFactor();
        boolean replace =
                equation.op() == BlendOp.ADD && from == BlendFactor.ONE && to == BlendFactor.ZERO;
        if (orderedDepth
                || replace
                || from == BlendFactor.ONE_MINUS_DST_COLOR && to == BlendFactor.ONE_MINUS_SRC_COLOR)
            return null;
        if (equation.op() != BlendOp.ADD)
            throw new IllegalArgumentException("Unsupported OIT blend: " + source.getLocation());
        boolean alpha = from == BlendFactor.SRC_ALPHA && to == BlendFactor.ONE_MINUS_SRC_ALPHA;
        boolean premultiplied = from == BlendFactor.ONE && to == BlendFactor.ONE_MINUS_SRC_ALPHA;
        boolean additive = from == BlendFactor.SRC_ALPHA && to == BlendFactor.ONE;
        boolean squared = from == BlendFactor.SRC_COLOR && to == BlendFactor.ONE;
        if (!alpha && !premultiplied && !additive && !squared) {
            throw new IllegalArgumentException("Unsupported OIT blend: " + source.getLocation());
        }
        var snippet =
                new Snippet(
                        source.getShaders(),
                        Optional.of(source.getShaderDefines()),
                        Optional.of(source.getBindGroupLayouts()),
                        new ColorTargetState[8],
                        0,
                        Optional.ofNullable(source.getDepthStencilState()),
                        Optional.of(source.getPolygonMode()),
                        Optional.of(source.isCull()),
                        source.vertexFormatPerBuffer.toArray(VertexFormat[]::new),
                        Optional.of(source.getPrimitiveTopology()),
                        source.pushConstantSize());
        var builder = RenderPipeline.builder(snippet);
        if (additive || squared) builder.withShaderDefine("OIT_ADDITIVE");
        if (squared) builder.withShaderDefine("NTM_OIT_SQUARED_COLOR");
        var set =
                OitPipelineSet.builder(
                        "ntm_"
                                + source.getLocation().getNamespace()
                                + "/"
                                + source.getLocation().getPath(),
                        builder);
        DepthStencilState depth = source.getDepthStencilState();
        if (depth == null) return set.withoutDepthTest().build();
        var tested =
                new DepthStencilState(
                        depth.depthTest(),
                        false,
                        depth.depthBiasScaleFactor(),
                        depth.depthBiasConstant());
        return set.withDepthBoundsModifier(stage -> stage.withDepthStencilState(tested))
                .withTransmittanceModifier(stage -> stage.withDepthStencilState(tested))
                .withAccumulateModifier(stage -> stage.withDepthStencilState(tested))
                .build();
    }
}

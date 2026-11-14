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
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.BiFunction;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class WorldRenderPipeline extends RenderPipeline implements OitPipelineOwner {
    private static final VarHandle NEXT_INDEX;
    private static int nextIndex;

    static {
        try {
            NEXT_INDEX =
                    MethodHandles.lookup()
                            .findStaticVarHandle(WorldRenderPipeline.class, "nextIndex", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final RenderPipeline UNTEXTURED_PIPELINE =
            untextured("pipeline/untextured", false);
    public static final RenderPipeline UNTEXTURED_CULL_PIPELINE =
            untextured("pipeline/untextured_cull", true);
    public static final RenderType UNTEXTURED =
            RenderType.create(
                    "ntm_untextured",
                    RenderSetup.builder(UNTEXTURED_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .affectsCrumbling()
                            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                            .createRenderSetup());
    public static final RenderType UNTEXTURED_CULL =
            RenderType.create(
                    "ntm_untextured_cull",
                    RenderSetup.builder(UNTEXTURED_CULL_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .affectsCrumbling()
                            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                            .createRenderSetup());
    public static final RenderPipeline ONE_SIDED_CUTOUT_PIPELINE =
            of(
                    RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                            .withLocation(RegistryUtil.id("pipeline/one_sided_cutout"))
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                            .withCull(false));
    private static final BiFunction<Identifier, Boolean, RenderType> ONE_SIDED_CUTOUT =
            Util.memoize(
                    (texture, affectsOutline) ->
                            RenderType.create(
                                    "ntm_one_sided_cutout",
                                    RenderSetup.builder(ONE_SIDED_CUTOUT_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .useLightmap()
                                            .useOverlay()
                                            .affectsCrumbling()
                                            .setOutline(
                                                    affectsOutline
                                                            ? RenderSetup.OutlineProperty
                                                                    .AFFECTS_OUTLINE
                                                            : RenderSetup.OutlineProperty.NONE)
                                            .createRenderSetup()));
    public static final RenderPipeline ONE_SIDED_TRANSLUCENT_PIPELINE =
            of(
                    RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                            .withLocation(RegistryUtil.id("pipeline/one_sided_translucent"))
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                            .withCull(false));
    private static final BiFunction<Identifier, Boolean, RenderType> ONE_SIDED_TRANSLUCENT =
            Util.memoize(
                    (texture, affectsOutline) ->
                            RenderType.create(
                                    "ntm_one_sided_translucent",
                                    RenderSetup.builder(ONE_SIDED_TRANSLUCENT_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .useLightmap()
                                            .useOverlay()
                                            .affectsCrumbling()
                                            .sortOnUpload()
                                            .setOutline(
                                                    affectsOutline
                                                            ? RenderSetup.OutlineProperty
                                                                    .AFFECTS_OUTLINE
                                                            : RenderSetup.OutlineProperty.NONE)
                                            .createRenderSetup()));
    public final int index = (int) NEXT_INDEX.getAndAdd(1);
    public final Family family;
    public final boolean fullbright;
    public final boolean overlay;
    public final boolean intensity;
    public final boolean fog;
    public final boolean translucent;
    public final boolean alphaAdditive;
    public final boolean emission;
    public final boolean glint;
    public final float alphaCutout;
    private final OitPipelineSet oitPipelines;

    private WorldRenderPipeline(RenderPipeline source) {
        this(source, family(source.getShaders().get(ShaderType.FRAGMENT).toString()));
    }

    private WorldRenderPipeline(RenderPipeline source, Family family) {
        this(source, family, false);
    }

    private WorldRenderPipeline(RenderPipeline source, Family family, boolean orderedDepth) {
        super(
                source.getLocation(),
                source.getShaders(),
                source.getShaderDefines(),
                source.getBindGroupLayouts(),
                source.getColorTargetStates().isEmpty()
                        ? new ColorTargetState[] {ColorTargetState.DEFAULT}
                        : source.getColorTargetStates().toArray(ColorTargetState[]::new),
                source.getDepthStencilState(),
                source.getPolygonMode(),
                source.isCull(),
                source.vertexFormatPerBuffer.toArray(VertexFormat[]::new),
                source.getPrimitiveTopology(),
                source.pushConstantSize(),
                source.getSortKey());
        String shader = source.getShaders().get(ShaderType.FRAGMENT).toString();
        this.family = family;
        if (family != Family.NATIVE_SKY
                && family(source.getShaders().get(ShaderType.VERTEX).toString()) != family)
            throw new IllegalArgumentException(shader);
        var flags = source.getShaderDefines().flags();
        fullbright =
                flags.contains("EMISSIVE")
                        || family != Family.ENTITY
                                && family != Family.PARTICLE
                                && family != Family.TEXT;
        overlay = family == Family.ENTITY && !flags.contains("NO_OVERLAY");
        intensity = family == Family.TEXT && flags.contains("IS_GRAYSCALE");
        fog = !shader.endsWith("_nofog") && family != Family.BASIC && family != Family.NATIVE_SKY;
        translucent =
                ((com.mojang.renderpearl.api.pipeline.ColorTargetState)
                                (source.getColorTargetStates().isEmpty()
                                        ? ColorTargetState.DEFAULT
                                        : source.getColorTargetStates().getFirst()))
                        .blendFunction()
                        .isPresent();
        alphaAdditive =
                ((com.mojang.renderpearl.api.pipeline.ColorTargetState)
                                (source.getColorTargetStates().isEmpty()
                                        ? ColorTargetState.DEFAULT
                                        : source.getColorTargetStates().getFirst()))
                        .blendFunction()
                        .filter(BlendFunction.LIGHTNING::equals)
                        .isPresent();
        emission = flags.contains("ADDITIVE_EMISSION");
        glint =
                ((com.mojang.renderpearl.api.pipeline.ColorTargetState)
                                (source.getColorTargetStates().isEmpty()
                                        ? ColorTargetState.DEFAULT
                                        : source.getColorTargetStates().getFirst()))
                        .blendFunction()
                        .filter(BlendFunction.GLINT::equals)
                        .isPresent();
        alphaCutout =
                Float.parseFloat(
                        source.getShaderDefines()
                                .values()
                                .getOrDefault(
                                        "ALPHA_CUTOUT",
                                        family == Family.PARTICLE || family == Family.TEXT
                                                ? "0.1"
                                                : "0"));
        var uniforms = BindGroupLayout.flattenUniforms(source.getBindGroupLayouts());
        assert !overlay
                || uniforms.stream()
                        .anyMatch(
                                uniform ->
                                        uniform.type() == UniformType.COMBINED_IMAGE_SAMPLER
                                                && uniform.name().equals("Sampler1"));
        oitPipelines =
                family == Family.NATIVE_SKY
                        ? null
                        : NativeOitPipelines.create(source, orderedDepth);
    }

    @Override
    public OitPipelineSet oitPipelines() {
        return oitPipelines;
    }

    @Override
    public boolean orderedBlend() {
        return translucent
                && oitPipelines == null
                && family != Family.NATIVE_SKY
                && getColorTargetStates().getFirst().writeMask() != ColorTargetState.WRITE_NONE;
    }

    private static Family family(String shader) {
        return switch (shader) {
            case "minecraft:core/entity",
                    "hbm:core/entity_nofog",
                    "hbm:core/entity_fade",
                    "hbm:core/entity_chunk" ->
                    Family.ENTITY;
            case "minecraft:core/particle",
                    "hbm:core/particle_nofog",
                    "hbm:core/particle_fade",
                    "hbm:core/particle_cutout" ->
                    Family.PARTICLE;
            case "minecraft:core/rendertype_lines", "hbm:core/rendertype_lines_nofog" ->
                    Family.LINES;
            case "minecraft:core/rendertype_lightning", "hbm:core/rendertype_lightning_nofog" ->
                    Family.LIGHTNING;
            case "minecraft:core/position_color" -> Family.BASIC;
            case "minecraft:core/text", "hbm:core/text_fade" -> Family.TEXT;
            default -> throw new IllegalArgumentException("Unmapped world shader: " + shader);
        };
    }

    public static RenderType oneSidedCutout(Identifier texture) {
        return oneSidedCutout(texture, true);
    }

    public static RenderType oneSidedCutout(Identifier texture, boolean affectsOutline) {
        return ONE_SIDED_CUTOUT.apply(texture, affectsOutline);
    }

    public static RenderType oneSidedTranslucent(Identifier texture) {
        return oneSidedTranslucent(texture, true);
    }

    public static RenderType oneSidedTranslucent(Identifier texture, boolean affectsOutline) {
        return ONE_SIDED_TRANSLUCENT.apply(texture, affectsOutline);
    }

    public static WorldRenderPipeline entity(Builder builder) {
        return entity(builder, false);
    }

    public static WorldRenderPipeline entity(Builder builder, boolean orderedDepth) {
        return new WorldRenderPipeline(builder.build(), Family.ENTITY, orderedDepth);
    }

    public static WorldRenderPipeline of(Builder builder) {
        return new WorldRenderPipeline(builder.build());
    }

    public static WorldRenderPipeline orderedDepth(Builder builder) {
        RenderPipeline source = builder.build();
        return new WorldRenderPipeline(
                source, family(source.getShaders().get(ShaderType.FRAGMENT).toString()), true);
    }

    public static WorldRenderPipeline nativeSky(RenderPipeline source) {
        return new WorldRenderPipeline(source, Family.NATIVE_SKY);
    }

    private static WorldRenderPipeline untextured(String location, boolean cull) {
        return of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(RegistryUtil.id(location))
                        .withVertexShader("core/entity")
                        .withFragmentShader("core/entity")
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(DepthStencilState.DEFAULT)
                        .withCull(cull));
    }

    public enum Family {
        ENTITY,
        PARTICLE,
        LINES,
        LIGHTNING,
        BASIC,
        TEXT,
        NATIVE_SKY
    }
}

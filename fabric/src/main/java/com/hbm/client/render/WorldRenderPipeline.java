// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.lib.Library;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.BiFunction;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class WorldRenderPipeline extends RenderPipeline {
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
                            .withLocation(Library.id("pipeline/one_sided_cutout"))
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
                            .withLocation(Library.id("pipeline/one_sided_translucent"))
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
    public final boolean glint;
    public final float alphaCutout;

    private WorldRenderPipeline(RenderPipeline source) {

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
        String shader = source.getFragmentShader().toString();
        family = family(shader);
        if (family(source.getVertexShader().toString()) != family)
            throw new IllegalArgumentException(shader);
        var flags = source.getShaderDefines().flags();
        var samplers = BindGroupLayout.flattenSamplers(source.getBindGroupLayouts());
        fullbright =
                flags.contains("EMISSIVE")
                        || family != Family.ENTITY
                                && family != Family.PARTICLE
                                && family != Family.TEXT;
        overlay = family == Family.ENTITY && !flags.contains("NO_OVERLAY");
        intensity = family == Family.TEXT && flags.contains("IS_GRAYSCALE");
        fog = !shader.endsWith("_nofog") && family != Family.BASIC;
        translucent = source.getColorTargetState().blendFunction().isPresent();
        glint =
                source.getColorTargetState()
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
        assert !overlay || samplers.contains("Sampler1");
    }

    private static Family family(String shader) {
        return switch (shader) {
            case "minecraft:core/entity", "hbm:core/entity_nofog", "hbm:core/entity_fade" ->
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

    public static WorldRenderPipeline of(RenderPipeline.Builder builder) {
        return new WorldRenderPipeline(builder.build());
    }

    private static WorldRenderPipeline untextured(String location, boolean cull) {
        return of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(Library.id(location))
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
        TEXT
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.iris;

import com.hbm.client.render.WorldRenderPipeline;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.platform.BlendOp;
import java.util.Arrays;
import java.util.BitSet;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisShadowProgram;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderOverrides;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import org.jspecify.annotations.Nullable;

public final class WorldShaderCompiler {
    private static final BitSet shadowAssigned = new BitSet();
    private static @Nullable WorldRenderPipeline active;
    private static @Nullable BlendModeOverride blend;

    private WorldShaderCompiler() {}

    public static int phase(IrisRenderingPipeline world) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) return 4;
        if (HandRenderer.INSTANCE.isActive())
            return HandRenderer.INSTANCE.isRenderingSolid() ? 2 : 3;
        return ShaderOverrides.isBlockEntities(world) ? 1 : 0;
    }

    public static ShaderKey key(WorldRenderPipeline pipeline, int phase) {
        if (phase == 4)
            return switch (pipeline.family) {
                case ENTITY -> ShaderKey.SHADOW_ENTITIES_CUTOUT;
                case PARTICLE -> ShaderKey.SHADOW_PARTICLES;
                case LINES -> ShaderKey.SHADOW_LINES;
                case LIGHTNING -> ShaderKey.SHADOW_LIGHTNING;
                case BASIC -> ShaderKey.SHADOW_BASIC_COLOR;
                case TEXT ->
                        pipeline.intensity
                                ? ShaderKey.SHADOW_TEXT_INTENSITY
                                : ShaderKey.SHADOW_TEXT;
            };
        return switch (pipeline.family) {
            case ENTITY -> {
                if (pipeline.glint) yield ShaderKey.GLINT;
                if (phase == 2) yield ShaderKey.HAND_CUTOUT;
                if (phase == 3) yield ShaderKey.HAND_TRANSLUCENT;
                if (pipeline.fullbright) yield ShaderKey.ENTITIES_EYES;
                if (phase == 1)
                    yield pipeline.translucent ? ShaderKey.BE_TRANSLUCENT : ShaderKey.BLOCK_ENTITY;
                yield pipeline.translucent
                        ? ShaderKey.ENTITIES_TRANSLUCENT
                        : ShaderKey.ENTITIES_SOLID;
            }
            case PARTICLE -> pipeline.translucent ? ShaderKey.PARTICLES_TRANS : ShaderKey.PARTICLES;
            case LINES -> ShaderKey.LINES;
            case LIGHTNING -> ShaderKey.LIGHTNING;
            case BASIC -> ShaderKey.BASIC_COLOR;
            case TEXT -> {
                if (pipeline.intensity)
                    yield phase == 1 ? ShaderKey.TEXT_INTENSITY_BE : ShaderKey.TEXT_INTENSITY;
                if (phase == 2) yield ShaderKey.HAND_TEXT;
                if (phase == 3) yield ShaderKey.HAND_TEXT_TRANSLUCENT;
                yield phase == 1 ? ShaderKey.TEXT_BE : ShaderKey.TEXT;
            }
        };
    }

    public static void assignShadow(WorldRenderPipeline pipeline) {
        if (shadowAssigned.get(pipeline.index)) return;
        ProgramId program = key(pipeline, 4).getProgram();
        IrisApi.getInstance()
                .assignPipelineShadow(
                        pipeline,
                        Arrays.stream(IrisShadowProgram.values())
                                .filter(candidate -> ProgramId.fromAPI(candidate) == program)
                                .findFirst()
                                .orElseThrow());
        shadowAssigned.set(pipeline.index);
    }

    public static void begin(WorldRenderPipeline pipeline, ProgramSource source, boolean shadow) {
        assert active == null;
        active = pipeline;

        blend =
                source.getDirectives()
                        .getBlendModeOverride()
                        .filter(value -> value != ProgramId.SpiderEyes.getBlendModeOverride())
                        .orElseGet(
                                () -> {
                                    if (shadow
                                            || pipeline.getColorTargetState()
                                                    .blendFunction()
                                                    .isEmpty()) return BlendModeOverride.OFF;
                                    var function =
                                            pipeline.getColorTargetState()
                                                    .blendFunction()
                                                    .orElseThrow();
                                    assert function.color().op() == BlendOp.ADD
                                            && function.alpha().op() == BlendOp.ADD;
                                    return new BlendModeOverride(
                                            new BlendMode(
                                                    GlConst.toGl(function.color().sourceFactor()),
                                                    GlConst.toGl(function.color().destFactor()),
                                                    GlConst.toGl(function.alpha().sourceFactor()),
                                                    GlConst.toGl(function.alpha().destFactor())));
                                });
    }

    public static void end() {
        active = null;
        blend = null;
    }

    public static BlendModeOverride blend(BlendModeOverride original) {
        return active == null ? original : blend;
    }

    public static ShaderAttributeInputs inputs(ShaderAttributeInputs original) {
        return active != null && active.family == WorldRenderPipeline.Family.ENTITY
                ? new ShaderAttributeInputs(true, true, active.overlay, !active.fullbright, true)
                : original;
    }
}

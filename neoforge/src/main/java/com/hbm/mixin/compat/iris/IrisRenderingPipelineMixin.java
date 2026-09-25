// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.iris;

import com.google.common.collect.ImmutableSet;
import com.hbm.client.render.TracerRibbon;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.client.render.WorldRenderPipeline;
import com.hbm.client.render.iris.RibbonCompiler;
import com.hbm.client.render.iris.WorldFallbackShader;
import com.hbm.client.render.iris.WorldShaderCompiler;
import com.hbm.interfaces.injected.RibbonPipeline;
import com.hbm.interfaces.injected.WorldPipeline;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTestFunction;
import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderCreator;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderSupplier;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public abstract class IrisRenderingPipelineMixin implements RibbonPipeline, WorldPipeline {
    @Shadow @Final private ProgramFallbackResolver resolver;
    @Shadow @Final private Set<GlProgram> loadedShaders;
    @Shadow @Final private RenderTargets renderTargets;
    @Shadow @Final private ImmutableSet<Integer> flippedAfterPrepare;
    @Shadow @Final private ImmutableSet<Integer> flippedAfterTranslucent;
    @Shadow @Final private Supplier<ShadowRenderTargets> shadowTargetsSupplier;
    @Unique private GlRenderPipeline hbm$litRibbon;
    @Unique private GlRenderPipeline hbm$emissiveRibbon;
    @Unique private GlRenderPipeline hbm$emissiveNoFogRibbon;
    @Unique private GlRenderPipeline[] hbm$programs = new GlRenderPipeline[0];

    @Shadow
    protected abstract ShaderSupplier createShader(
            String name,
            ShaderKey key,
            ProgramSource source,
            ProgramId programId,
            AlphaTest alpha,
            VertexFormat format,
            FogMode fog,
            boolean intensity,
            boolean fullbright,
            boolean glint,
            boolean text,
            boolean ie,
            Patch patch)
            throws IOException;

    @Shadow
    protected abstract ShaderSupplier createShadowShader(
            String name,
            ShaderKey key,
            ProgramSource source,
            ProgramId programId,
            AlphaTest alpha,
            VertexFormat format,
            boolean intensity,
            boolean fullbright,
            boolean text,
            boolean ie,
            Patch patch)
            throws IOException;

    @Override
    public GlRenderPipeline hbm$ribbon(RenderPipeline pipeline) {
        boolean fullbright = pipeline != WeaponRenderTypes.TRACER_PIPELINE;
        boolean fog = pipeline != WeaponRenderTypes.TRACER_FULLBRIGHT_NO_FOG_PIPELINE;
        GlRenderPipeline cached =
                !fog ? hbm$emissiveNoFogRibbon : fullbright ? hbm$emissiveRibbon : hbm$litRibbon;
        if (cached != null) return cached;
        ShaderKey key = fullbright ? ShaderKey.ENTITIES_EYES_TRANS : ShaderKey.ENTITIES_TRANSLUCENT;
        String name =
                (fullbright ? "hbm_ribbon_emissive" : "hbm_ribbon_lit") + (fog ? "" : "_nofog");
        FogMode fogMode = fog ? FogMode.PER_VERTEX : FogMode.OFF;
        RibbonCompiler.begin(fullbright);
        try {
            ProgramSource source = resolver.resolveNullable(key.getProgram());
            ShaderSupplier compiled;
            if (source != null) {
                compiled =
                        createShader(
                                name,
                                key,
                                source,
                                key.getProgram(),
                                AlphaTests.OFF,
                                TracerRibbon.FORMAT,
                                fogMode,
                                false,
                                fullbright,
                                false,
                                false,
                                false,
                                Patch.VANILLA);
            } else {
                compiled =
                        ShaderCreator.createFallback(
                                name,
                                key,
                                renderTargets.createGbufferFramebuffer(
                                        flippedAfterPrepare, new int[] {0}),
                                renderTargets.createGbufferFramebuffer(
                                        flippedAfterTranslucent, new int[] {0}),
                                AlphaTests.OFF,
                                TracerRibbon.FORMAT,
                                RibbonCompiler.BLEND,
                                (IrisRenderingPipeline) (Object) this,
                                fogMode,
                                false,
                                false,
                                false,
                                false,
                                fullbright);
            }
            GlProgram program = compiled.shader().get();
            loadedShaders.add(program);
            cached = new GlRenderPipeline(pipeline, program);
            if (!fog) hbm$emissiveNoFogRibbon = cached;
            else if (fullbright) hbm$emissiveRibbon = cached;
            else hbm$litRibbon = cached;
            return cached;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            RibbonCompiler.end();
        }
    }

    @Override
    public GlRenderPipeline hbm$program(WorldRenderPipeline pipeline) {
        var world = (IrisRenderingPipeline) (Object) this;
        int phase = WorldShaderCompiler.phase(world);
        int index = pipeline.index * 5 + phase;
        if (index >= hbm$programs.length) hbm$programs = Arrays.copyOf(hbm$programs, index + 160);
        GlRenderPipeline cached = hbm$programs[index];
        if (cached != null) return cached;
        WorldShaderCompiler.assignShadow(pipeline);
        ShaderKey key = WorldShaderCompiler.key(pipeline, phase);
        ProgramSource source = resolver.resolveNullable(key.getProgram());

        String name =
                "hbm_"
                        + pipeline.getLocation().getPath().replace('/', '_')
                        + (phase == 4 ? "_shadow" : "_" + phase);
        GlProgram program;
        if (source == null) {
            var before =
                    phase == 4
                            ? shadowTargetsSupplier
                                    .get()
                                    .createShadowFramebuffer(ImmutableSet.of(), new int[] {0})
                            : renderTargets.createGbufferFramebuffer(
                                    flippedAfterPrepare, new int[] {0});
            var after =
                    phase == 4
                            ? before
                            : renderTargets.createGbufferFramebuffer(
                                    flippedAfterTranslucent, new int[] {0});
            program = new WorldFallbackShader(name, pipeline, world, before, after);
        } else {
            AlphaTest alpha =
                    pipeline.alphaCutout == 0
                            ? AlphaTests.OFF
                            : new AlphaTest(AlphaTestFunction.GEQUAL, pipeline.alphaCutout);
            WorldShaderCompiler.begin(pipeline, source, phase == 4);
            try {
                ShaderSupplier compiled =
                        phase == 4
                                ? createShadowShader(
                                        name,
                                        key,
                                        source,
                                        key.getProgram(),
                                        alpha,
                                        pipeline.getVertexFormatBinding(0),
                                        key.isIntensity(),
                                        pipeline.fullbright,
                                        key.isText(),
                                        false,
                                        Patch.VANILLA)
                                : createShader(
                                        name,
                                        key,
                                        source,
                                        key.getProgram(),
                                        alpha,
                                        pipeline.getVertexFormatBinding(0),
                                        pipeline.fog ? FogMode.PER_VERTEX : FogMode.OFF,
                                        key.isIntensity(),
                                        pipeline.fullbright,
                                        false,
                                        key.isText(),
                                        false,
                                        Patch.VANILLA);
                program = compiled.shader().get();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            } finally {
                WorldShaderCompiler.end();
            }
        }
        loadedShaders.add(program);
        return hbm$programs[index] = new GlRenderPipeline(pipeline, program);
    }
}

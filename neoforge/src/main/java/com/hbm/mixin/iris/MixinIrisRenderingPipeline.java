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

package com.hbm.mixin.iris;

import com.google.common.collect.ImmutableSet;
import com.hbm.client.render.TracerRibbon;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.client.render.WorldRenderPipeline;
import com.hbm.client.render.iris.RibbonCompiler;
import com.hbm.client.render.iris.WorldFallbackShader;
import com.hbm.client.render.iris.WorldShaderCompiler;
import com.hbm.interfaces.injected.RibbonPipeline;
import com.hbm.interfaces.injected.WorldPipeline;
import com.hbm.mixin.compat.iris.ProgramFallbackResolverAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline.CreateInfo;
import com.mojang.renderpearl.backend.opengl.GlDevice;
import com.mojang.renderpearl.backend.opengl.GlProgram;
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;
import com.mojang.renderpearl.backend.opengl.VertexArray;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import dev.engine_room.flywheel.backend.compile.RenderPipelineCompiler;
import dev.engine_room.flywheel.iris.compile.ContractProgram;
import dev.engine_room.flywheel.iris.compile.ContractProgramSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTestFunction;
import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.programs.ShaderCreator;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderSupplier;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.RenderTargets;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
public abstract class MixinIrisRenderingPipeline implements RibbonPipeline, WorldPipeline {
    @Unique
    private static final Set<WorldRenderingPhase> SKY_PHASES =
            EnumSet.of(
                    WorldRenderingPhase.CUSTOM_SKY,
                    WorldRenderingPhase.SKY,
                    WorldRenderingPhase.SUNSET,
                    WorldRenderingPhase.SUN,
                    WorldRenderingPhase.MOON,
                    WorldRenderingPhase.STARS,
                    WorldRenderingPhase.VOID);

    @Shadow @Final private ProgramFallbackResolver resolver;
    @Shadow @Final private Set<GlProgram> loadedShaders;
    @Shadow @Final private RenderTargets renderTargets;
    @Shadow @Final private ImmutableSet<Integer> flippedAfterPrepare;
    @Shadow @Final private ImmutableSet<Integer> flippedAfterTranslucent;
    @Shadow @Final private Supplier<ShadowRenderTargets> shadowTargetsSupplier;
    @Unique private CompiledRenderPipeline hbm$litRibbon;
    @Unique private CompiledRenderPipeline hbm$emissiveRibbon;
    @Unique private CompiledRenderPipeline hbm$emissiveNoFogRibbon;
    @Unique private CompiledRenderPipeline[] hbm$programs = new CompiledRenderPipeline[0];

    @Unique
    private CompiledRenderPipeline hbm$compile(RenderPipeline pipeline, GlProgram program) {
        var descriptions = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
        var indices = new Object2IntOpenHashMap<String>();
        for (int index = 0; index < descriptions.size(); index++) {
            indices.put(descriptions.get(index).name(), index);
        }
        var formats = pipeline.getVertexFormatBindings();
        var buffers = new ArrayList<CreateInfo.VertexBuffer>();
        var attributes = new ArrayList<CreateInfo.AttribBinding>();
        for (int slot = 0; slot < formats.size(); slot++) {
            VertexFormat format = formats.get(slot);
            if (format == null) continue;
            buffers.add(
                    new CreateInfo.VertexBuffer(
                            slot, format.getVertexSize(), format.getStepRate()));
            String previousName = null;
            int previousLocation = -1;
            for (var element : format.getElements()) {
                String name = element.name();
                int location;
                if (name.equals(previousName)) {
                    location = previousLocation < 0 ? -1 : previousLocation + 1;
                } else {
                    location = GL20C.glGetAttribLocation(program.getProgramId(), "iris_" + name);
                    if (location < 0)
                        location = GL20C.glGetAttribLocation(program.getProgramId(), name);
                }
                if (location >= 0)
                    attributes.add(
                            new CreateInfo.AttribBinding(
                                    slot, location, element.offset(), element.format()));
                previousName = name;
                previousLocation = location;
            }
        }
        var info =
                new CreateInfo(
                        pipeline.getLocation().toString(),
                        List.of(),
                        buffers,
                        attributes,
                        descriptions,
                        pipeline.pushConstantSize(),
                        pipeline.getDepthStencilState(),
                        pipeline.getPolygonMode(),
                        pipeline.isCull(),
                        pipeline.getColorTargetStates(),
                        pipeline.getPrimitiveTopology());
        var device = (GlDevice) ((FrontendGpuDevice) RenderSystem.getDevice()).backend;
        var vertexArray =
                VertexArray.createSource(GL.getCapabilities(), new HashSet<>())
                        .apply(program, info);
        var backend = new GlRenderPipeline(device, info, program, vertexArray);
        return new FrontendRenderPipeline(
                info.name(),
                backend,
                formats,
                indices,
                descriptions,
                info.colorTargetStates(),
                info.depthStencilState() != null,
                info.pushConstantsSize());
    }

    @Inject(method = "destroyShaders", at = @At("HEAD"))
    private void hbm$releaseNativePipelines(CallbackInfo ci) {
        for (CompiledRenderPipeline compiled : hbm$programs) {
            if (compiled != null)
                RenderPipelineCompiler.releaseGuestPipeline((FrontendRenderPipeline) compiled);
        }
        if (hbm$litRibbon != null)
            RenderPipelineCompiler.releaseGuestPipeline((FrontendRenderPipeline) hbm$litRibbon);
        if (hbm$emissiveRibbon != null)
            RenderPipelineCompiler.releaseGuestPipeline(
                    (FrontendRenderPipeline) hbm$emissiveRibbon);
        if (hbm$emissiveNoFogRibbon != null)
            RenderPipelineCompiler.releaseGuestPipeline(
                    (FrontendRenderPipeline) hbm$emissiveNoFogRibbon);
        Arrays.fill(hbm$programs, null);
        hbm$litRibbon = hbm$emissiveRibbon = hbm$emissiveNoFogRibbon = null;
    }

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
    public CompiledRenderPipeline hbm$ribbon(RenderPipeline pipeline) {
        boolean fullbright = pipeline != WeaponRenderTypes.TRACER_PIPELINE;
        boolean fog = pipeline != WeaponRenderTypes.TRACER_FULLBRIGHT_NO_FOG_PIPELINE;
        CompiledRenderPipeline cached =
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
            cached = hbm$compile(pipeline, program);
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
    public CompiledRenderPipeline hbm$program(WorldRenderPipeline pipeline) {
        var world = (IrisRenderingPipeline) (Object) this;
        int phase = WorldShaderCompiler.phase(world);
        boolean nativeSky = pipeline.family == WorldRenderPipeline.Family.NATIVE_SKY;
        if (nativeSky
                && (phase != 0
                        || !world.isBeforeTranslucent
                        || !SKY_PHASES.contains(world.getPhase()))) {
            throw new UnsupportedOperationException(
                    "native sky requires the main Iris sky before translucents: phase="
                            + phase
                            + ", worldPhase="
                            + world.getPhase()
                            + ", before="
                            + world.isBeforeTranslucent);
        }
        int index = pipeline.index * 5 + phase;
        if (index >= hbm$programs.length) hbm$programs = Arrays.copyOf(hbm$programs, index + 160);
        CompiledRenderPipeline cached = hbm$programs[index];
        if (cached != null) return cached;
        if (nativeSky) {
            var target = renderTargets.createGbufferFramebuffer(flippedAfterPrepare, new int[] {0});
            String name = "hbm_native_sky_" + pipeline.getLocation().getPath().replace('/', '_');
            GlProgram program = new WorldFallbackShader(name, pipeline, world, target, target);
            loadedShaders.add(program);
            return hbm$programs[index] = hbm$compile(pipeline, program);
        }
        WorldShaderCompiler.assignShadow(pipeline);
        ShaderKey key = WorldShaderCompiler.key(pipeline, phase);
        ProgramSource source = resolver.resolveNullable(key.getProgram());
        if (phase != 4
                && pipeline.emission
                && pipeline.alphaAdditive
                && (pipeline.family == WorldRenderPipeline.Family.ENTITY
                        || pipeline.family == WorldRenderPipeline.Family.PARTICLE
                        || pipeline.family == WorldRenderPipeline.Family.LIGHTNING)) {
            var contracts =
                    (ContractProgramSet)
                            ((java.lang.Object)
                                    (((ProgramFallbackResolverAccessor) resolver).hbm$programs()));
            ProgramSource emission =
                    contracts.flywheel$contractSource(
                            pipeline.family == WorldRenderPipeline.Family.LIGHTNING
                                    ? ContractProgram.GBUFFERS_NATIVE_ADDITIVE_COLOR
                                    : ContractProgram.GBUFFERS_NATIVE_ADDITIVE);
            if (emission != null) source = emission;
        }
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
                    pipeline.alphaCutout != 0
                            ? new AlphaTest(AlphaTestFunction.GEQUAL, pipeline.alphaCutout)
                            : pipeline.getShaderDefines().flags().contains("DISCARD_ZERO_ALPHA")
                                    ? new AlphaTest(AlphaTestFunction.GREATER, 0)
                                    : AlphaTests.OFF;
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
        return hbm$programs[index] = hbm$compile(pipeline, program);
    }
}

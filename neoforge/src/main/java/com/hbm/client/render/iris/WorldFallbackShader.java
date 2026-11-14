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

package com.hbm.client.render.iris;

import com.hbm.client.render.WorldRenderPipeline;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout.UniformDescription;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.backend.opengl.GlProgram;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import com.mojang.renderpearl.backend.opengl.Uniform.Sampler;
import com.mojang.renderpearl.backend.opengl.Uniform.Ubo;
import com.mojang.renderpearl.backend.opengl.Uniform.Utb;
import com.mojang.renderpearl.backend.opengl.Uniform;
import dev.engine_room.flywheel.backend.compile.NativeShaderPreprocessor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import net.irisshaders.iris.compat.SkipList;
import net.irisshaders.iris.gl.blending.DepthColorStorage;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.mixinterface.GlProgramBindings;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.IrisProgram;
import net.irisshaders.iris.pipeline.programs.ShaderCreator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL31C;

public final class WorldFallbackShader extends GlProgram implements IrisProgram, GlProgramBindings {
    private final IrisRenderingPipeline world;
    private final GlFramebuffer before;
    private final GlFramebuffer after;
    private boolean setUp;
    private final List<UniformDescription> uniformDescriptions;

    public WorldFallbackShader(
            String name,
            WorldRenderPipeline pipeline,
            IrisRenderingPipeline world,
            GlFramebuffer before,
            GlFramebuffer after) {
        super(link(name, pipeline), name);
        this.world = world;
        this.before = before;
        this.after = after;
        ((net.irisshaders.iris.mixinterface.ShaderInstanceInterface) (java.lang.Object) (this))
                .setShouldSkip(SkipList.NONE);
        uniformDescriptions = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
        bindNativeLayouts();
    }

    private void bindNativeLayouts() {
        int program = getProgramId();
        int nextBlock = 0;
        int nextSampler = 0;
        GlStateManager._glUseProgram(program);
        for (UniformDescription description : uniformDescriptions) {
            Uniform uniform =
                    switch (description.type()) {
                        case UNIFORM_BUFFER -> {
                            int block = nextBlock++;
                            int index = iris$getBlockIndex(program, description.name());
                            if (index != GL31C.GL_INVALID_INDEX)
                                GL31C.glUniformBlockBinding(program, index, block);
                            yield new Ubo(block);
                        }
                        case COMBINED_IMAGE_SAMPLER -> {
                            int sampler = nextSampler++;
                            GlStateManager._glUniform1i(
                                    GlStateManager._glGetUniformLocation(
                                            program, description.name()),
                                    sampler);
                            yield new Sampler(sampler);
                        }
                        case TEXEL_BUFFER -> {
                            int sampler = nextSampler++;
                            GlStateManager._glUniform1i(
                                    GlStateManager._glGetUniformLocation(
                                            program, description.name()),
                                    sampler);
                            yield new Utb(sampler, description.gpuFormat());
                        }
                    };
            super.uniforms.add(uniform);
        }
        GlStateManager._glUseProgram(0);
    }

    @Override
    public void iris$setupBindings(List<UniformDescription> descriptions, int pushConstantsSize) {
        if (pushConstantsSize != 0 || !uniformDescriptions.equals(descriptions)) {
            throw new IllegalStateException(
                    "Native fallback binding contract changed: " + getDebugLabel());
        }
    }

    private static int link(String name, WorldRenderPipeline pipeline) {
        var shaders = Minecraft.getInstance().getShaderManager();
        var defines =
                pipeline.getShaderDefines()
                        .withOverrides(
                                ShaderDefines.builder()
                                        .define("gl_VertexIndex", "gl_VertexID")
                                        .build());
        String vertex =
                NativeShaderPreprocessor.preprocess(
                        nativeSource(
                                ((RenderPipeline) pipeline).getShaders().get(ShaderType.VERTEX),
                                ShaderType.VERTEX),
                        name + ".vsh",
                        ShaderType.VERTEX,
                        defines);
        String fragment =
                NativeShaderPreprocessor.preprocess(
                        nativeSource(
                                ((RenderPipeline) pipeline).getShaders().get(ShaderType.FRAGMENT),
                                ShaderType.FRAGMENT),
                        name + ".fsh",
                        ShaderType.FRAGMENT,
                        defines);
        return ShaderCreator.link(
                        name,
                        vertex,
                        null,
                        null,
                        null,
                        fragment,
                        pipeline.getVertexFormatBinding(0),
                        true)
                .getFinally();
    }

    private static String nativeSource(Identifier id, ShaderType type) {
        try {
            return Minecraft.getInstance()
                    .getResourceManager()
                    .getResourceOrThrow(type.idConverter().idToFile(id))
                    .readAllAsString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void iris$clearState() {
        setUp = false;
    }

    @Override
    public int iris$getBlockIndex(int program, CharSequence name) {
        return GL31C.glGetUniformBlockIndex(program, name);
    }

    @Override
    public boolean iris$isSetUp() {
        return setUp;
    }

    @Override
    public void iris$setupState(List<UniformDescription> descriptions) {
        setUp = true;
        DepthColorStorage.unlockDepthColor();
        GlStateManager._glUseProgram(getProgramId());
        (world.isBeforeTranslucent ? before : after).bind();
    }
}

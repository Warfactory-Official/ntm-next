// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.iris;

import com.hbm.client.render.WorldRenderPipeline;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.HashMap;
import net.irisshaders.iris.compat.SkipList;
import net.irisshaders.iris.gl.blending.DepthColorStorage;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.mixinterface.ShaderInstanceInterface;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.IrisProgram;
import net.irisshaders.iris.pipeline.programs.ShaderCreator;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL31C;

public final class WorldFallbackShader extends GlProgram implements IrisProgram {
    private final IrisRenderingPipeline world;
    private final GlFramebuffer before;
    private final GlFramebuffer after;
    private boolean setUp;

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
        ((ShaderInstanceInterface) (Object) this).setShouldSkip(SkipList.NONE);
        setupBindGroupLayouts(pipeline.getBindGroupLayouts());
    }

    private static int link(String name, WorldRenderPipeline pipeline) {
        var shaders = Minecraft.getInstance().getShaderManager();
        String vertex =
                GlslPreprocessor.injectDefines(
                        shaders.getShader(pipeline.getVertexShader(), ShaderType.VERTEX),
                        pipeline.getShaderDefines());
        String fragment =
                GlslPreprocessor.injectDefines(
                        shaders.getShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT),
                        pipeline.getShaderDefines());
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
    public void iris$setupState(
            HashMap<String, GlRenderPass.TextureViewAndSampler> samplers, GpuTextureView albedo) {
        setUp = true;
        DepthColorStorage.unlockDepthColor();
        GlStateManager._glUseProgram(getProgramId());
        (world.isBeforeTranslucent ? before : after).bind();
    }
}

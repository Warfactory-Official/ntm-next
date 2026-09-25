// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.iris;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.client.render.WorldRenderPipeline;
import com.hbm.interfaces.injected.RibbonPipeline;
import com.hbm.interfaces.injected.WorldPipeline;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.vertices.ImmediateState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice", priority = 900)
public class GlDeviceMixin {

    @Inject(method = "getOrCompilePipeline", at = @At("HEAD"), cancellable = true)
    private void hbm$ribbonProgram(
            RenderPipeline pipeline, CallbackInfoReturnable<GlRenderPipeline> cir) {
        if (pipeline != WeaponRenderTypes.TRACER_PIPELINE
                && pipeline != WeaponRenderTypes.TRACER_FULLBRIGHT_PIPELINE
                && pipeline != WeaponRenderTypes.TRACER_FULLBRIGHT_NO_FOG_PIPELINE) return;
        if (!ImmediateState.bypass
                && Iris.getPipelineManager().getPipelineNullable()
                        instanceof IrisRenderingPipeline world
                && world.shouldOverrideShaders()) {
            cir.setReturnValue(((RibbonPipeline) world).hbm$ribbon(pipeline));
        }
    }

    @Inject(method = "getOrCompilePipeline", at = @At("HEAD"), cancellable = true)
    private void hbm$worldProgram(
            RenderPipeline pipeline, CallbackInfoReturnable<GlRenderPipeline> cir) {
        if (!(pipeline instanceof WorldRenderPipeline owned) || ImmediateState.bypass) return;
        if (Iris.getPipelineManager().getPipelineNullable() instanceof IrisRenderingPipeline world
                && world.shouldOverrideShaders()) {
            cir.setReturnValue(((WorldPipeline) world).hbm$program(owned));
        }
    }
}

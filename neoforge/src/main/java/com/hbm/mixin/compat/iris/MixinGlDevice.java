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

package com.hbm.mixin.compat.iris;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.client.render.WorldRenderPipeline;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.vertices.ImmediateState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice", priority = 900)
public class MixinGlDevice {
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
            cir.setReturnValue(
                    (GlRenderPipeline)
                            ((com.hbm.interfaces.injected.RibbonPipeline) (world))
                                    .hbm$ribbon(pipeline));
        }
    }

    @Inject(method = "getOrCompilePipeline", at = @At("HEAD"), cancellable = true)
    private void hbm$worldProgram(
            RenderPipeline pipeline, CallbackInfoReturnable<GlRenderPipeline> cir) {
        if (!(pipeline instanceof WorldRenderPipeline owned) || ImmediateState.bypass) return;
        if (owned.family == WorldRenderPipeline.Family.NATIVE_SKY
                && (RenderSystem.outputColorTextureOverride != null
                        || RenderSystem.outputDepthTextureOverride != null)) return;
        if (Iris.getPipelineManager().getPipelineNullable() instanceof IrisRenderingPipeline world
                && world.shouldOverrideShaders()) {
            cir.setReturnValue(
                    (GlRenderPipeline)
                            ((com.hbm.interfaces.injected.WorldPipeline) (world))
                                    .hbm$program(owned));
        }
    }
}

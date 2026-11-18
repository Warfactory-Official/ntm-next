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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.vertices.ImmediateState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderSystem.class, priority = 900)
public class MixinRenderSystem {
    @Inject(method = "getCompiledPipelineNullable", at = @At("HEAD"), cancellable = true)
    private static void hbm$nativePrograms(
            RenderPipeline pipeline, CallbackInfoReturnable<CompiledRenderPipeline> cir) {
        if (ImmediateState.bypass
                || !(Iris.getPipelineManager().getPipelineNullable()
                        instanceof IrisRenderingPipeline world)
                || !world.shouldOverrideShaders()) return;
        if (pipeline == WeaponRenderTypes.TRACER_PIPELINE
                || pipeline == WeaponRenderTypes.TRACER_FULLBRIGHT_PIPELINE
                || pipeline == WeaponRenderTypes.TRACER_FULLBRIGHT_NO_FOG_PIPELINE) {
            cir.setReturnValue(
                    ((com.hbm.interfaces.injected.RibbonPipeline) (world)).hbm$ribbon(pipeline));
        } else if (pipeline instanceof WorldRenderPipeline owned) {
            cir.setReturnValue(
                    ((com.hbm.interfaces.injected.WorldPipeline) (world)).hbm$program(owned));
        }
    }
}

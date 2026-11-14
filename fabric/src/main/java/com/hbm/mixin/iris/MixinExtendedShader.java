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

import com.hbm.client.render.iris.RibbonCompiler;
import com.hbm.client.render.iris.WorldShaderCompiler;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ExtendedShader.class, remap = false)
public class MixinExtendedShader {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static BlendModeOverride hbm$ribbonBlend(BlendModeOverride original) {
        return RibbonCompiler.active() ? RibbonCompiler.BLEND : original;
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static BlendModeOverride hbm$worldBlend(BlendModeOverride original) {
        return WorldShaderCompiler.blend(original);
    }
}

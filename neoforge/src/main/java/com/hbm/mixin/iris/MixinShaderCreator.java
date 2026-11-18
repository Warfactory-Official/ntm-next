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
import com.hbm.client.render.iris.RibbonShaderTransform;
import com.hbm.client.render.iris.WorldShaderCompiler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.pipeline.programs.PartialShader;
import net.irisshaders.iris.pipeline.programs.ShaderCreator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ShaderCreator.class, remap = false)
public class MixinShaderCreator {
    @WrapMethod(method = "link")
    private static PartialShader hbm$ribbon(
            String name,
            String vertex,
            String geometry,
            String control,
            String evaluation,
            String fragment,
            VertexFormat format,
            boolean fallback,
            Operation<PartialShader> original) {
        if (RibbonCompiler.active()) {
            vertex = RibbonShaderTransform.vertex(vertex, fallback);
            fragment = RibbonShaderTransform.fragment(fragment);
        }
        return original.call(
                name, vertex, geometry, control, evaluation, fragment, format, fallback);
    }

    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true)
    private static ShaderAttributeInputs hbm$virtualInputs(ShaderAttributeInputs original) {
        return RibbonCompiler.active()
                ? new ShaderAttributeInputs(true, true, false, !RibbonCompiler.fullbright(), true)
                : original;
    }

    @ModifyVariable(
            method = {"create", "createShadow"},
            at = @At("HEAD"),
            argsOnly = true)
    private static ShaderAttributeInputs hbm$worldInputs(ShaderAttributeInputs original) {
        return WorldShaderCompiler.inputs(original);
    }
}

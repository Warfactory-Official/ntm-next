// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.iris;

import com.hbm.client.render.iris.RibbonCompiler;
import com.hbm.client.render.iris.RibbonShaderTransform;
import com.hbm.client.render.iris.WorldShaderCompiler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.pipeline.programs.PartialShader;
import net.irisshaders.iris.pipeline.programs.ShaderCreator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(value = ShaderCreator.class, remap = false)
public class ShaderCreatorMixin {

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

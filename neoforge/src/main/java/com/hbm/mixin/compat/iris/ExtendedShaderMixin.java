// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.iris;

import com.hbm.client.render.iris.RibbonCompiler;
import com.hbm.client.render.iris.WorldShaderCompiler;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(value = ExtendedShader.class, remap = false)
public class ExtendedShaderMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static BlendModeOverride hbm$ribbonBlend(BlendModeOverride original) {
        return RibbonCompiler.active() ? RibbonCompiler.BLEND : original;
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static BlendModeOverride hbm$worldBlend(BlendModeOverride original) {
        return WorldShaderCompiler.blend(original);
    }
}

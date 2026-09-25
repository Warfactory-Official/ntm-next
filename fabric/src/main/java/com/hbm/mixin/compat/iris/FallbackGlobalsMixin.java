// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.iris;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import net.irisshaders.iris.pipeline.fallback.ShaderSynthesizer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(value = ShaderSynthesizer.class, remap = false)
public class FallbackGlobalsMixin {

    @ModifyConstant(
            method = "vsh",
            constant =
                    @Constant(
                            stringValue =
                                    """
            layout(std140) uniform Globals {
                vec2 ScreenSize;
                float GlintAlpha;
                float GameTime;
                int MenuBlurRadius;
            };
            """))
    private static String hbm$currentGlobals(String original) {
        try (var stream =
                Minecraft.getInstance()
                        .getResourceManager()
                        .open(Identifier.withDefaultNamespace("shaders/include/globals.glsl"))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceFirst("(?m)^#version[^\\r\\n]*", "");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

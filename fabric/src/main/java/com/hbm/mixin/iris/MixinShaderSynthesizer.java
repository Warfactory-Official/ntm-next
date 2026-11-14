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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import net.irisshaders.iris.pipeline.fallback.ShaderSynthesizer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ShaderSynthesizer.class, remap = false)
public class MixinShaderSynthesizer {
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

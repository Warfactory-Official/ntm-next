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

package com.hbm.client.render;

import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;

public final class SlagTextures {
    private static final Identifier SOURCE = RegistryUtil.id("textures/block/slag.png");
    private static volatile Map<Material, Identifier> TEXTURES = Map.of();

    private SlagTextures() {}

    public static void reload(ResourceManager resources) {
        Minecraft minecraft = Minecraft.getInstance();
        assert minecraft.isSameThread();
        var textureManager = minecraft.getTextureManager();
        Map<Material, Identifier> previous = TEXTURES;
        TEXTURES = Map.of();
        for (Identifier texture : previous.values()) textureManager.release(texture);

        Map<Material, Identifier> next = new HashMap<>();
        try (var stream = resources.getResourceOrThrow(SOURCE).open();
                NativeImage source = NativeImage.read(stream)) {
            for (Material material : MaterialRegistry.ordered()) {
                if (material.lightColor() == material.darkColor()) continue;
                Identifier texture = RegistryUtil.id("dynamic/slag/" + material.name());
                NativeImage recolored = source.mappedCopy(pixel -> recolor(pixel, material));
                textureManager.register(
                        texture, new DynamicTexture(() -> texture.toString(), recolored));
                next.put(material, texture);
            }
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        TEXTURES = Map.copyOf(next);
    }

    public static Identifier texture(Material material) {
        Identifier texture = TEXTURES.get(material);
        if (texture == null)
            throw new IllegalStateException("Slag textures have not been published");
        return texture;
    }

    private static int recolor(int pixel, Material material) {
        return ARGB.color(
                ARGB.alpha(pixel),
                channel(
                        ARGB.red(pixel),
                        ARGB.red(material.lightColor()),
                        ARGB.red(material.darkColor())),
                channel(
                        ARGB.green(pixel),
                        ARGB.green(material.lightColor()),
                        ARGB.green(material.darkColor())),
                channel(
                        ARGB.blue(pixel),
                        ARGB.blue(material.lightColor()),
                        ARGB.blue(material.darkColor())));
    }

    private static int channel(int component, int lighter, int darker) {
        double position = (component - 255D) / (80D - 255D);
        return (int) (lighter + position * (darker - lighter)) & 255;
    }
}

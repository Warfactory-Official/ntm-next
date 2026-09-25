// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.lib.Library;
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
    private static final Identifier SOURCE = Library.id("textures/block/slag.png");
    private static volatile Map<NTMMaterial, Identifier> TEXTURES = Map.of();

    private SlagTextures() {}

    public static void reload(ResourceManager resources) {
        Minecraft minecraft = Minecraft.getInstance();
        assert minecraft.isSameThread();
        var textureManager = minecraft.getTextureManager();
        Map<NTMMaterial, Identifier> previous = TEXTURES;
        TEXTURES = Map.of();
        for (Identifier texture : previous.values()) textureManager.release(texture);

        Map<NTMMaterial, Identifier> next = new HashMap<>();
        try (var stream = resources.getResourceOrThrow(SOURCE).open();
                NativeImage source = NativeImage.read(stream)) {
            for (NTMMaterial material : Mats.orderedList) {
                if (material.solidColorLight == material.solidColorDark) continue;
                Identifier texture = Library.id("dynamic/slag/" + material.tagPath);
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

    public static Identifier texture(NTMMaterial material) {
        Identifier texture = TEXTURES.get(material);
        if (texture == null)
            throw new IllegalStateException("Slag textures have not been published");
        return texture;
    }

    private static int recolor(int pixel, NTMMaterial material) {
        return ARGB.color(
                ARGB.alpha(pixel),
                channel(
                        ARGB.red(pixel),
                        ARGB.red(material.solidColorLight),
                        ARGB.red(material.solidColorDark)),
                channel(
                        ARGB.green(pixel),
                        ARGB.green(material.solidColorLight),
                        ARGB.green(material.solidColorDark)),
                channel(
                        ARGB.blue(pixel),
                        ARGB.blue(material.solidColorLight),
                        ARGB.blue(material.solidColorDark)));
    }

    private static int channel(int component, int lighter, int darker) {
        double position = (component - 255D) / (80D - 255D);
        return (int) (lighter + position * (darker - lighter)) & 255;
    }
}

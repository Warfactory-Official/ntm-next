// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.platform.Services;
import com.mojang.blaze3d.platform.NativeImage;
import dev.engine_room.flywheel.api.backend.BackendManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public final class VisualTextures {
    private static final Map<Identifier, Boolean> TRANSLUCENT = new ConcurrentHashMap<>();
    private static final boolean SODIUM = Services.PLATFORM.isModLoaded("sodium");
    private static final Set<TextureAtlasSprite> ANIMATED = ConcurrentHashMap.newKeySet();

    private VisualTextures() {}

    static void animated(TextureAtlasSprite sprite) {
        if (SODIUM && sprite.contents().isAnimated()) ANIMATED.add(sprite);
    }

    public static void activateSprites() {
        if (SODIUM && BackendManager.isBackendOn()) SodiumSprites.activate();
    }

    public static boolean translucentTexture(Identifier texture) {
        return TRANSLUCENT.computeIfAbsent(
                texture,
                id -> {
                    if (id.equals(TextureAtlas.LOCATION_BLOCKS)) return false;
                    try (var stream =
                                    Minecraft.getInstance()
                                            .getResourceManager()
                                            .getResourceOrThrow(id)
                                            .open();
                            var image = NativeImage.read(stream)) {
                        for (int y = 0; y < image.getHeight(); y++) {
                            for (int x = 0; x < image.getWidth(); x++) {
                                int alpha = image.getPixel(x, y) >>> 24;
                                if (alpha != 0 && alpha != 255) return true;
                            }
                        }
                        return false;
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                });
    }

    static void reload() {
        TRANSLUCENT.clear();
        ANIMATED.clear();
    }

    private static final class SodiumSprites {
        static void activate() {
            for (var sprite : ANIMATED) SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }
    }
}

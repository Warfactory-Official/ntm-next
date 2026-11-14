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

package com.hbm.client.render.flywheel;

import com.hbm.platform.Platform;
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
    private static final boolean SODIUM = Platform.isModLoaded("sodium");
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

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

package com.hbm.client.model;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;

public final class QuadLighting {
    public static final int VANILLA = 0;
    private static final int HEADER = 0xFF << 24 | 3 << 22;
    public static final int OWN_BLOCK = 0x48 << 24 | 1 << 22;
    private static final int CELL = 0x48 << 24 | 2 << 22;

    private QuadLighting() {}

    public static int cell(int dx, int dy, int dz) {
        assert dx >= -8 && dx < 8 && dz >= -8 && dz < 8 && dy >= -8192 && dy < 8192;
        return CELL | (dx & 0xF) << 18 | (dz & 0xF) << 14 | dy & 0x3FFF;
    }

    public static boolean isCell(int origin) {
        return (origin & HEADER) == CELL;
    }

    public static boolean isOwnBlock(int origin) {
        return (origin & ~15) == OWN_BLOCK;
    }

    public static int fabricTag(BakedQuad.MaterialInfo material) {
        int origin =
                ((com.hbm.interfaces.injected.QuadLightOriginAccess) (java.lang.Object) (material))
                        .hbm$lightOrigin();
        return isOwnBlock(origin) ? ownBlockTag(material.lightEmission()) : origin;
    }

    public static int ownBlockTag(int emission) {
        assert emission >= 0 && emission <= 15;
        return OWN_BLOCK | emission;
    }

    public static int emission(int tag) {
        return tag & 15;
    }

    public static int applyEmission(int light, int tag) {
        int floor = emission(tag) << 4;
        return LightCoordsUtil.smoothPack(
                Math.max(LightCoordsUtil.smoothBlock(light), floor),
                Math.max(LightCoordsUtil.smoothSky(light), floor));
    }

    public static boolean isOffset(int origin) {
        return isCell(origin) && (origin & 0x3FFFFF) != 0;
    }

    public static int dx(int origin) {
        return origin << 10 >> 28;
    }

    public static int dy(int origin) {
        return origin << 18 >> 18;
    }

    public static int dz(int origin) {
        return origin << 14 >> 28;
    }

    public static BakedQuad.MaterialInfo ownBlock(BakedQuad.MaterialInfo material) {
        ((com.hbm.interfaces.injected.QuadLightOriginAccess) (java.lang.Object) (material))
                .hbm$setLightOrigin(OWN_BLOCK);
        return material;
    }

    public static boolean usesOwnBlock(BakedQuad.MaterialInfo material) {
        return isOwnBlock(
                ((com.hbm.interfaces.injected.QuadLightOriginAccess) (java.lang.Object) (material))
                        .hbm$lightOrigin());
    }

    public static boolean usesCell(BakedQuad.MaterialInfo material) {
        return isCell(
                ((com.hbm.interfaces.injected.QuadLightOriginAccess) (java.lang.Object) (material))
                        .hbm$lightOrigin());
    }

    public static BakedQuad.MaterialInfo copy(
            BakedQuad.MaterialInfo info, int origin, boolean ambientOcclusion) {
        final TextureAtlasSprite sprite = info.sprite();
        final RenderType itemType = info.itemRenderType();
        final int tint = info.tintIndex();
        final int emission = info.lightEmission();
        var copy =
                new BakedQuad.MaterialInfo(
                        sprite,
                        info.layer(),
                        itemType,
                        tint,
                        info.shade(),
                        emission,
                        info.ambientOcclusion() && ambientOcclusion);
        ((com.hbm.interfaces.injected.QuadLightOriginAccess) (java.lang.Object) (copy))
                .hbm$setLightOrigin(origin);
        return copy;
    }
}

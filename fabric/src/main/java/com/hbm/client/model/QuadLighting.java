// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import net.minecraft.client.resources.model.geometry.BakedQuad;

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
        material.hbm$setLightOrigin(OWN_BLOCK);
        return material;
    }

    public static boolean usesOwnBlock(BakedQuad.MaterialInfo material) {
        return material.hbm$lightOrigin() == OWN_BLOCK;
    }

    public static boolean usesCell(BakedQuad.MaterialInfo material) {
        return isCell(material.hbm$lightOrigin());
    }

    public static BakedQuad.MaterialInfo copy(
            BakedQuad.MaterialInfo info, int origin, boolean ambientOcclusion) {

        var copy =
                new BakedQuad.MaterialInfo(
                        info.sprite(),
                        info.layer(),
                        info.itemRenderType(),
                        info.tintIndex(),
                        info.shade(),
                        info.lightEmission());

        copy.hbm$setLightOrigin(origin);
        return copy;
    }
}

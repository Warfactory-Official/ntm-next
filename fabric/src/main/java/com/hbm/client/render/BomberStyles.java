// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.render.loader.HFRWavefrontObject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;

public final class BomberStyles {

    public record Style(
            Supplier<HFRWavefrontObject> mesh, Identifier texture, float scale, float yaw) {}

    private static final Map<Integer, Style> STYLES = new HashMap<>();

    private BomberStyles() {}

    public static void register(
            int style,
            Supplier<HFRWavefrontObject> mesh,
            Identifier texture,
            float scale,
            float yaw) {
        if (STYLES.putIfAbsent(style, new Style(mesh, texture, scale, yaw)) != null) {
            throw new IllegalStateException("duplicate bomber rendering for style " + style);
        }
    }

    static Style get(int style) {
        return STYLES.get(style);
    }
}

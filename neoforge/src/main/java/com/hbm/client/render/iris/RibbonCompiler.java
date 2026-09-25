// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.iris;

import java.util.Map;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import org.lwjgl.opengl.GL11C;

public final class RibbonCompiler {
    public static final BlendModeOverride BLEND =
            new BlendModeOverride(
                    new BlendMode(
                            GL11C.GL_SRC_ALPHA,
                            GL11C.GL_ONE_MINUS_SRC_ALPHA,
                            GL11C.GL_ONE,
                            GL11C.GL_ONE_MINUS_SRC_ALPHA));
    private static boolean active;
    private static boolean fullbright;
    static Map<String, String> colors = Map.of();

    private RibbonCompiler() {}

    public static void begin(boolean emissive) {
        assert !active;
        active = true;
        fullbright = emissive;
    }

    public static void end() {
        active = false;
        colors = Map.of();
    }

    public static boolean active() {
        return active;
    }

    public static boolean fullbright() {
        return fullbright;
    }
}

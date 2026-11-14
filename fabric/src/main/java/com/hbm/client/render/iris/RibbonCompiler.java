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

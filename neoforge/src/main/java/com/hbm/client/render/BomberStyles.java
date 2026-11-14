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

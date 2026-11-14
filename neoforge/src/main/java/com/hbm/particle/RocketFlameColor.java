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

package com.hbm.particle;

public record RocketFlameColor(
        float r, float g, float b, boolean fadeR, boolean fadeG, boolean fadeB, int light) {
    public static final RocketFlameColor FLAME =
            new RocketFlameColor(1F, 0.6F, 0F, true, true, true, 240);

    public static RocketFlameColor tinted(float r, float g, float b) {
        return new RocketFlameColor(
                r != 0F ? r : FLAME.r,
                g != 0F ? g : FLAME.g,
                b != 0F ? b : FLAME.b,
                true,
                true,
                true,
                FLAME.light);
    }

    public float red(float fade, float add) {
        return (fadeR ? r * fade : r) + add;
    }

    public float green(float fade, float add) {
        return (fadeG ? g * fade : g) + add;
    }

    public float blue(float fade, float add) {
        return (fadeB ? b * fade : b) + add;
    }
}

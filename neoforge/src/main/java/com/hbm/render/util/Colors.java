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

package com.hbm.render.util;

import net.minecraft.util.ARGB;

public final class Colors {
    private Colors() {}

    public static int argb(float alpha, float red, float green, float blue) {
        return argb(
                (int) (alpha * 255F),
                (int) (red * 255F),
                (int) (green * 255F),
                (int) (blue * 255F));
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return ARGB.color(
                Math.clamp(alpha, 0, 255),
                Math.clamp(red, 0, 255),
                Math.clamp(green, 0, 255),
                Math.clamp(blue, 0, 255));
    }
}

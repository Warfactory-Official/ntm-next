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

package com.hbm.items.machine;

import net.minecraft.world.item.DyeColor;

public enum ChemicalDyeType {
    BLACK(DyeColor.BLACK, 0x1E1B1B),
    RED(DyeColor.RED, 0xB3312C),
    GREEN(DyeColor.GREEN, 0x3B511A),
    BROWN(DyeColor.BROWN, 0x51301A),
    BLUE(DyeColor.BLUE, 0x253192),
    PURPLE(DyeColor.PURPLE, 0x7B2FBE),
    CYAN(DyeColor.CYAN, 0x287697),
    SILVER(DyeColor.LIGHT_GRAY, 0xABABAB),
    GRAY(DyeColor.GRAY, 0x434343),
    PINK(DyeColor.PINK, 0xD88198),
    LIME(DyeColor.LIME, 0x41CD34),
    YELLOW(DyeColor.YELLOW, 0xDECF2A),
    LIGHTBLUE(DyeColor.LIGHT_BLUE, 0x6689D3),
    MAGENTA(DyeColor.MAGENTA, 0xC354CD),
    ORANGE(DyeColor.ORANGE, 0xEB8844),
    WHITE(DyeColor.WHITE, 0xF0F0F0);

    public final DyeColor dye;
    public final int rgb;

    ChemicalDyeType(DyeColor dye, int rgb) {
        this.dye = dye;
        this.rgb = rgb;
    }
}

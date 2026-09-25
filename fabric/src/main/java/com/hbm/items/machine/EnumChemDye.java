// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import net.minecraft.world.item.DyeColor;

public enum EnumChemDye {
    BLACK(0x1E1B1B, DyeColor.BLACK),
    RED(0xB3312C, DyeColor.RED),
    GREEN(0x3B511A, DyeColor.GREEN),
    BROWN(0x51301A, DyeColor.BROWN),
    BLUE(0x253192, DyeColor.BLUE),
    PURPLE(0x7B2FBE, DyeColor.PURPLE),
    CYAN(0x287697, DyeColor.CYAN),
    SILVER(0xABABAB, DyeColor.LIGHT_GRAY),
    GRAY(0x434343, DyeColor.GRAY),
    PINK(0xD88198, DyeColor.PINK),
    LIME(0x41CD34, DyeColor.LIME),
    YELLOW(0xDECF2A, DyeColor.YELLOW),
    LIGHTBLUE(0x6689D3, DyeColor.LIGHT_BLUE),
    MAGENTA(0xC354CD, DyeColor.MAGENTA),
    ORANGE(0xEB8844, DyeColor.ORANGE),
    WHITE(0xF0F0F0, DyeColor.WHITE);

    public final int color;

    public final DyeColor dye;

    EnumChemDye(int color, DyeColor dye) {
        this.color = color;
        this.dye = dye;
    }
}

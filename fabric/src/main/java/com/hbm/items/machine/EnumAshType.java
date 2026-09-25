// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import net.minecraft.world.item.DyeColor;

public enum EnumAshType {
    WOOD(DyeColor.LIGHT_GRAY),
    COAL(DyeColor.BLACK),
    MISC(DyeColor.GRAY),
    FLY(DyeColor.BROWN),
    SOOT(DyeColor.BLACK),
    FULLERENE(DyeColor.MAGENTA);

    public final DyeColor dye;

    EnumAshType(DyeColor dye) {
        this.dye = dye;
    }
}

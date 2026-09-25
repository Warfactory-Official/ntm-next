// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import net.minecraft.world.item.DyeColor;

public enum EnumTarType {
    CRUDE(DyeColor.BLACK),
    CRACK(DyeColor.BLACK),
    COAL(DyeColor.GRAY),
    WOOD(DyeColor.BROWN),
    WAX(DyeColor.CYAN),
    PARAFFIN(DyeColor.WHITE);

    public final DyeColor dye;

    EnumTarType(DyeColor dye) {
        this.dye = dye;
    }
}

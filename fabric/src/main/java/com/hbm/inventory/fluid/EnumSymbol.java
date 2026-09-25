// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

public enum EnumSymbol {
    NONE(0, 0),
    NOWATER(195, 63),
    RADIATION(195, 2),
    ACID(195, 124),
    ANTIMATTER(73, 185),
    CRYOGENIC(134, 185),
    OXIDIZER(12, 185),
    ASPHYXIANT(195, 185);

    public final int x;
    public final int y;

    EnumSymbol(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

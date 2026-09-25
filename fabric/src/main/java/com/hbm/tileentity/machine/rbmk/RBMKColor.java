// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import java.util.Locale;

public enum RBMKColor {
    RED,
    YELLOW,
    GREEN,
    BLUE,
    PURPLE;

    public static final RBMKColor[] VALUES = values();

    public String texture() {
        return name().toLowerCase(Locale.ROOT);
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import java.util.Locale;

public enum EnumBatterySC {
    EMPTY(0),
    WASTE(150),
    RA226(200),
    TC99(500),
    CO60(750),
    PU238(1_000),
    PO210(1_250),
    AU198(1_500),
    PB209(2_000),
    AM241(2_500);

    public static final EnumBatterySC[] VALUES = values();
    public final long power;
    public final String id = name().toLowerCase(Locale.ROOT);

    EnumBatterySC(long power) {
        this.power = power;
    }
}

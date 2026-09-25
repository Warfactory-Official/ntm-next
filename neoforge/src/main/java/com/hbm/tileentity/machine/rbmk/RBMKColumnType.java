// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

public enum RBMKColumnType {
    BLANK(0),
    FUEL(10),
    FUEL_SIM(90),
    CONTROL(20),
    CONTROL_AUTO(30),
    BOILER(40),
    MODERATOR(50),
    ABSORBER(60),
    REFLECTOR(70),
    OUTGASSER(80),
    BREEDER(100),
    STORAGE(110),
    COOLER(120),
    HEATEX(130);

    public final int offset;

    RBMKColumnType(int offset) {
        this.offset = offset;
    }
}

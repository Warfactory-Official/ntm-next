// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine.upgrade;

public enum UpgradeType {
    SPEED(3),
    EFFECT(3),
    POWER(3),
    FORTUNE(3),
    AFTERBURN(3),
    OVERDRIVE(3),
    NULLIFIER(1),
    SCREAM(1),

    EJECTOR(3),
    STACK(3),
    SPECIAL(Integer.MAX_VALUE);

    public static final int UNCAPPED = Integer.MAX_VALUE;

    public static final UpgradeType[] VALUES = values();

    public final int maxLevel;

    UpgradeType(int maxLevel) {
        this.maxLevel = maxLevel;
    }
}

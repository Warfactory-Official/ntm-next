// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum ConveyorBend implements StringRepresentable {
    STRAIGHT,
    LEFT,
    RIGHT;

    public static final ConveyorBend[] VALUES = values();

    private final String id = name().toLowerCase(Locale.ROOT);

    public ConveyorBend next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public ConveyorBend mirrored() {
        return switch (this) {
            case STRAIGHT -> STRAIGHT;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}

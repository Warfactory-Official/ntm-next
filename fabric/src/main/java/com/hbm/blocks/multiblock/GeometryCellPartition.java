// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

public enum GeometryCellPartition {
    STATIC(false, false, false),
    DOOR(true, false, false),
    DOOR_SEALED(true, true, false),

    RAIL(false, false, true);

    private final boolean opens;
    private final boolean seals;
    private final boolean waterlogs;

    GeometryCellPartition(boolean opens, boolean seals, boolean waterlogs) {
        this.opens = opens;
        this.seals = seals;
        this.waterlogs = waterlogs;
    }

    public static GeometryCellPartition of(boolean opens, boolean seals, boolean waterlogs) {
        for (GeometryCellPartition part : values()) {
            if (part.opens == opens && part.seals == seals && part.waterlogs == waterlogs)
                return part;
        }
        throw new IllegalStateException(
                "no geometry-cell partition for opens="
                        + opens
                        + " seals="
                        + seals
                        + " waterlogs="
                        + waterlogs
                        + "; add one with its own measured radix");
    }

    public boolean opens() {
        return opens;
    }

    public boolean seals() {
        return seals;
    }

    public boolean waterlogs() {
        return waterlogs;
    }
}

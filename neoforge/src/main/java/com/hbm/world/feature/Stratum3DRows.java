// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class Stratum3DRows {
    private static final int SLOTS = 64;
    private static final VarHandle SLOT = MethodHandles.arrayElementVarHandle(Row[].class);

    public final double scaleAlong;
    public final double scaleAcross;
    private final Row[] slots = new Row[SLOTS];

    public Stratum3DRows(double scaleAlong, double scaleAcross) {
        this.scaleAlong = scaleAlong;
        this.scaleAcross = scaleAcross;
    }

    record Row(int key, double[] values, double[] maxAbs) {}

    Row get(int key) {
        Row row = (Row) SLOT.getAcquire(this.slots, Math.floorMod(key >> 4, SLOTS));
        return row != null && row.key == key ? row : null;
    }

    void put(Row row) {
        SLOT.setRelease(this.slots, Math.floorMod(row.key >> 4, SLOTS), row);
    }
}

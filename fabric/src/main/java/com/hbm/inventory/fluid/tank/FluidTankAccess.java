// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.tank;

public interface FluidTankAccess {
    FluidTankNTM[] EMPTY_SNAPSHOT = new FluidTankNTM[0];
    FluidTankAccess EMPTY =
            new FluidTankAccess() {
                @Override
                public int tankCount() {
                    return 0;
                }

                @Override
                public FluidTankNTM tank(int index) {
                    throw new IndexOutOfBoundsException(index);
                }
            };

    int tankCount();

    FluidTankNTM tank(int index);

    static FluidTankAccess of(Object owner) {
        return owner instanceof FluidTankAccess tanks ? tanks : EMPTY;
    }

    static FluidTankNTM[] snapshot(Object owner) {
        FluidTankAccess tanks = of(owner);
        int count = tanks.tankCount();
        if (count == 0) return EMPTY_SNAPSHOT;
        FluidTankNTM[] snapshot = new FluidTankNTM[count];
        for (int i = 0; i < snapshot.length; i++) snapshot[i] = tanks.tank(i);
        return snapshot;
    }
}

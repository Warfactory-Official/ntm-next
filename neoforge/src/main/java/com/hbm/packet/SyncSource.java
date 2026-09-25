// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

public interface SyncSource {
    boolean syncBound();

    void syncChanged(int mask);

    default void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    default void bindSyncValue(Object value, int mask, long units) {
        SyncBindings.bindUnits(this, value, mask, units);
    }

    default void syncArrayChanged(Object value, int index, int mask, long units) {
        if (index >= 0 && syncBound() && value instanceof Object[] array)
            bindSyncValue(array[index], mask, units);
        if (units == 0) syncChanged(mask);
        else syncUnitsChanged(mask, units);
    }

    void bindSync(SyncSource owner, int mask);

    default void bindSyncUnits(SyncSource owner, int mask, long units) {
        bindSync(owner, mask);
    }

    void unbindSync(SyncSource owner);
}

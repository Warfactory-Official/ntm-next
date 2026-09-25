// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import io.netty.buffer.ByteBuf;

public interface SyncUnitSchema {
    long syncUnitMask();

    default boolean initialMatchesSyncUnits() {
        return true;
    }

    default long syncEventUnits() {
        return 0;
    }

    void writeSyncUnit(int unit, ByteBuf output);

    void readSyncUnit(int unit, ByteBuf input);

    default void writeInitialSyncUnit(int unit, ByteBuf output) {
        writeSyncUnit(unit, output);
    }

    default void writeInitialExtras(ByteBuf output) {}

    default void readInitialExtras(ByteBuf input) {}

    default void afterSyncUnits(long units) {}

    default void afterInitialSyncUnits() {
        afterSyncUnits(syncUnitMask());
    }

    default boolean afterRelayedSyncUnits(long units, boolean initial) {
        return false;
    }
}

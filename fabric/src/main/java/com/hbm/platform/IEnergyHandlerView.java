// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

public interface IEnergyHandlerView {

    Object handlerIdentity();

    long amount();

    long capacity();

    long insertable();

    long extractable();

    long insert(long maxFe, long feQuantum, boolean simulate);

    long extract(long maxFe, long feQuantum, boolean simulate);

    default boolean canInsert() {
        return insertable() > 0;
    }

    default boolean canExtract() {
        return extractable() > 0;
    }
}

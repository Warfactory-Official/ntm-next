// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.maps;

@FunctionalInterface
public interface LongObjectConsumer<T> {
    void accept(long key, T value);
}

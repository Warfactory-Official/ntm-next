// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.maps;

@FunctionalInterface
public interface LongObjectRefConsumer<V, R> {
    void accept(long key, V value, R ref);
}

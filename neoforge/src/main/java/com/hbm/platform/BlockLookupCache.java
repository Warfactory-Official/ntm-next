// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface BlockLookupCache<T> {

    @Nullable T find();
}

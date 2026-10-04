// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import org.jspecify.annotations.Nullable;

public interface FuseContainerOwner {
    default @Nullable Object hbm$fuseOwner() {
        throw new AbstractMethodError();
    }

    default void hbm$fuseOwner(@Nullable Object owner) {
        throw new AbstractMethodError();
    }

    default void hbm$trackSlots(Object source) {
        throw new AbstractMethodError();
    }
}

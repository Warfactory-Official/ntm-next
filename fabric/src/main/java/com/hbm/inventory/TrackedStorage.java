// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.function.Consumer;

public interface TrackedStorage {
    void inventorySources(Consumer<Object> sources);

    default void refreshInventorySources() {}
}

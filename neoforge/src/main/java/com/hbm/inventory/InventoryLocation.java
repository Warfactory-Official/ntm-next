// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import org.jspecify.annotations.Nullable;

public interface InventoryLocation {
    @Nullable Object inventoryCarrier();
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public interface KeyedSlotSource extends SlotSource {
    Object2IntOpenHashMap<Object> inventoryKeySlots();

    Object[] inventorySlotKeys();

    void inventorySlotKeys(Object[] keys);

    IntArrayList inventoryFreeSlots();
}

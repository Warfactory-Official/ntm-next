// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import net.minecraft.world.item.ItemStack;

public interface StoredItemSlots {
    int storedSlotCount();

    ItemStack storedItem(int slot);

    boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement);
}

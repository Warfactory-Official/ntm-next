// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.slot;

import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotUpgrade extends Slot {

    public SlotUpgrade(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return ItemMachineUpgrade.isUpgrade(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}

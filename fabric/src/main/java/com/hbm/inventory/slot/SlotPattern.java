// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotPattern extends Slot {

    private boolean allowStackSize = false;

    public SlotPattern(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public SlotPattern allowStackSize() {
        this.allowStackSize = true;
        return this;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return allowStackSize ? 64 : 1;
    }

    @Override
    public void set(ItemStack stack) {

        if (!stack.isEmpty()) {
            stack = allowStackSize ? stack.copy() : stack.copyWithCount(1);
        }
        super.set(stack);
    }
}

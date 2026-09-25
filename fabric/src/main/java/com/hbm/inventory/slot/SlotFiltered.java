// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.slot;

import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotFiltered extends Slot {

    public static final Predicate<ItemStack> NONE = stack -> false;

    private final Predicate<ItemStack> filter;

    public SlotFiltered(Container container, int slot, int x, int y, Predicate<ItemStack> filter) {
        super(container, slot, x, y);
        this.filter = filter;
    }

    public static SlotFiltered gated(Container container, int slot, int x, int y) {
        return new SlotFiltered(
                container, slot, x, y, stack -> container.canPlaceItem(slot, stack));
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return filter.test(stack);
    }
}

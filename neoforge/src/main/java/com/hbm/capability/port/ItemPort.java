// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability.port;

import com.hbm.capability.NtmCapabilities;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record ItemPort(int[] slots, @Nullable SlotStack insert, @Nullable SlotStack extract)
        implements PortView {

    private static final int[] NO_SLOTS = new int[0];
    private static final ItemPort NONE = new ItemPort(NO_SLOTS, null, null);

    public static ItemPort none() {
        return NONE;
    }

    public static ItemPort both(int... slots) {
        return new ItemPort(slots, (slot, stack) -> true, (slot, stack) -> true);
    }

    public static ItemPort insertOnly(SlotStack accepts, int... slots) {
        return new ItemPort(slots, accepts, null);
    }

    public static ItemPort extractOnly(int... slots) {
        return new ItemPort(slots, null, (slot, stack) -> true);
    }

    @Override
    public <T> @Nullable T as(Class<T> type, NtmCapabilities.CapRole role) {
        return type.isInstance(this) ? type.cast(this) : null;
    }

    public boolean isEmpty() {
        return slots.length == 0;
    }

    public boolean canInsert(int slot, ItemStack stack) {
        return insert != null && insert.test(slot, stack);
    }

    public boolean canExtract(int slot, ItemStack stack) {
        return extract != null && extract.test(slot, stack);
    }

    @FunctionalInterface
    public interface SlotStack {
        boolean test(int slot, ItemStack stack);
    }
}

/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.capability.port;

import com.hbm.capability.NtmCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
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

    public static boolean mayInsert(
            WorldlyContainer owner,
            @Nullable ItemPort port,
            int slot,
            ItemStack offered,
            @Nullable Direction side) {
        return port != null
                ? port.canInsert(slot, offered) && owner.canPlaceItem(slot, offered)
                : owner.canPlaceItemThroughFace(slot, offered, side);
    }

    public static boolean mayExtract(
            WorldlyContainer owner, @Nullable ItemPort port, int slot, @Nullable Direction side) {
        ItemStack held = owner.getItem(slot);
        return port != null
                ? port.canExtract(slot, held)
                : side == null || owner.canTakeItemThroughFace(slot, held, side);
    }

    @FunctionalInterface
    public interface SlotStack {
        boolean test(int slot, ItemStack stack);
    }
}

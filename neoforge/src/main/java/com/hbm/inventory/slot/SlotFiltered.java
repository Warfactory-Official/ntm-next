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

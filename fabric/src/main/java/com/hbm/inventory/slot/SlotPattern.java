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

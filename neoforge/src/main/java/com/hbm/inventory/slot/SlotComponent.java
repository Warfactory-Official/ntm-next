// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class SlotComponent extends Slot {

    public SlotComponent(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }
}

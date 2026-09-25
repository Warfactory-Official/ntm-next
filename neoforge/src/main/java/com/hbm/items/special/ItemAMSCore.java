// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.main.Polaroid;
import net.minecraft.world.item.ItemStack;

public class ItemAMSCore extends ItemCustomLore {

    private final int multiplier;
    private final boolean foilOnBalefireDay;

    public ItemAMSCore(int multiplier, boolean foilOnBalefireDay, Properties props) {
        super(props);
        this.multiplier = multiplier;
        this.foilOnBalefireDay = foilOnBalefireDay;
    }

    public static int getMultiplier(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.multiplier : 0;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foilOnBalefireDay && Polaroid.isBalefireDay();
    }
}

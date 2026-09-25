// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.items.ModDataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemMarshmallow extends Item {

    public ItemMarshmallow(Properties props) {
        super(props);
    }

    public static ItemStack roasted(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.ROASTED.get(), true);
        return stack;
    }

    public static boolean isRaw(ItemStack stack) {
        return !stack.getOrDefault(ModDataComponents.ROASTED.get(), false);
    }
}

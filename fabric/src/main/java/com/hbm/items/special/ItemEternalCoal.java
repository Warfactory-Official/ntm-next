// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.StackRemainderItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public final class ItemEternalCoal extends StackRemainderItem {
    public ItemEternalCoal(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemStackTemplate remainder(ItemStack consumed) {
        return new ItemStackTemplate(this);
    }
}

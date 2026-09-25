// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.StackRemainderItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public class ItemCraftingDegradation extends StackRemainderItem {

    public ItemCraftingDegradation(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected @Nullable ItemStackTemplate remainder(ItemStack consumed) {
        ItemStack tool = consumed.copyWithCount(1);
        if (tool.isDamageableItem()) {
            int damage = tool.getDamageValue() + 1;

            if (damage > tool.getMaxDamage()) return null;
            tool.setDamageValue(damage);
        }
        return ItemStackTemplate.fromNonEmptyStack(tool);
    }
}

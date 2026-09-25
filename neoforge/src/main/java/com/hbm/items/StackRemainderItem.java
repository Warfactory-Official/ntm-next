// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public abstract class StackRemainderItem extends Item {

    protected StackRemainderItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public final @Nullable ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        return remainder(
                switch (instance) {
                    case ItemStack stack -> stack;
                    case ItemStackTemplate template -> template.create();
                    default -> throw new IllegalArgumentException(instance.getClass().getName());
                });
    }

    protected abstract @Nullable ItemStackTemplate remainder(ItemStack consumed);
}

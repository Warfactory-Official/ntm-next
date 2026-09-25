// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public abstract class StackRemainderItem extends Item implements FabricItem {

    protected StackRemainderItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public final @Nullable ItemStackTemplate getCraftingRemainder(ItemStack stack) {
        return remainder(stack);
    }

    protected abstract @Nullable ItemStackTemplate remainder(ItemStack consumed);
}

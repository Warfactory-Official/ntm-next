// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin.storage;

import com.hbm.fabric.integration.StoredItemTransactions;
import com.hbm.inventory.ListSlotSource;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.api.items.InventoryBase;

@Mixin(value = InventoryBase.class, remap = false, priority = 500)
public abstract class MixinInventoryBase implements ListSlotSource {
    @Shadow private NonNullList<ItemStack> stacks;

    @Override
    public boolean directStoredMutation() {
        return false;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (!StoredItemTransactions.isIdle() || stacks.get(slot) != expected) return false;
        ((Container) (Object) this).setItem(slot, replacement);
        return true;
    }
}

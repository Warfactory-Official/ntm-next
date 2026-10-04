// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import appeng.util.inv.AppEngInternalInventory;
import com.hbm.inventory.ListSlotSource;
import com.hbm.inventory.SlotSource;
import com.hbm.inventory.StoredInventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "appeng.blockentity.inventory.AppEngCellInventory", remap = false)
public abstract class MixinAppEngCellInventory implements SlotSource {
    @Shadow @Final private AppEngInternalInventory inv;

    @Shadow
    public abstract int size();

    @Shadow
    public abstract ItemStack getStackInSlot(int slot);

    @Shadow
    public abstract void setItemDirect(int slot, ItemStack stack);

    @Override
    public int storedSlotCount() {
        return size();
    }

    @Override
    public ItemStack storedItem(int slot) {
        return getStackInSlot(slot);
    }

    @Override
    public void bindInventorySlots() {
        ListSlotSource inner = (ListSlotSource) inv;
        var items = inner.inventorySlotList();
        if (!(items instanceof StoredInventory watched) || !watched.owns(this))
            inner.inventorySlotList(StoredInventory.track(this, items));
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || getStackInSlot(slot) != expected) return false;
        setItemDirect(slot, replacement);
        return true;
    }
}

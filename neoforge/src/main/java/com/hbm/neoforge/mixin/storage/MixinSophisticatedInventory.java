// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.interfaces.StoredItemSlots;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public abstract class MixinSophisticatedInventory implements StoredItemSlots {
    @Shadow
    public abstract ItemStack getInternalStack(int slot);

    @Shadow
    public abstract boolean isInfinite(int slot);

    @Shadow
    public abstract void setStackInSlotInternal(int slot, ItemStack stack);

    @Override
    public int storedSlotCount() {
        return ((ItemStacksResourceHandler) (Object) this).size();
    }

    @Override
    public ItemStack storedItem(int slot) {
        return isInfinite(slot) ? ItemStack.EMPTY : getInternalStack(slot);
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || isInfinite(slot)
                || getInternalStack(slot) != expected) return false;
        setStackInSlotInternal(slot, replacement);
        return true;
    }
}

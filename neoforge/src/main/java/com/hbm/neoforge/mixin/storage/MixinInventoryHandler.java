// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.inventory.InventoryAlias;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.SlotSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InventoryHandler.class, remap = false)
public abstract class MixinInventoryHandler implements SlotSource, InventoryAlias {
    @Shadow
    public abstract ItemStack getInternalStack(int slot);

    @Shadow
    public abstract boolean isInfinite(int slot);

    @Shadow
    public abstract void setStackInSlotInternal(int slot, ItemStack stack);

    @Shadow @Final private ContainerContents.InventoryData inventoryData;
    @Shadow private boolean persistent;

    @Override
    public Object inventoryIdentity() {
        return inventoryData;
    }

    @Override
    public void prepareStoredSlot(int slot) {
        if (persistent
                && Transaction.getLifecycle() == Transaction.Lifecycle.NONE
                && !ItemStack.matches(inventoryData.stacks().get(slot), getInternalStack(slot)))
            setStackInSlotInternal(slot, getInternalStack(slot));
    }

    @Override
    public int storedSlotCount() {
        return ((ItemStacksResourceHandler) (Object) this).size();
    }

    @Override
    public ItemStack storedItem(int slot) {
        return isInfinite(slot) ? ItemStack.EMPTY : getInternalStack(slot);
    }

    @Override
    public boolean physicalSlot(int slot) {
        return slot >= 0 && slot < storedSlotCount() && !isInfinite(slot);
    }

    @Inject(method = "onFilterItemsChanged", at = @At("RETURN"))
    private void hbm$slotOwnershipChanged(CallbackInfo ci) {
        InventoryChanges.layoutChanged(this);
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

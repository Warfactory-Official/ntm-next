// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin.storage;

import com.hbm.fabric.integration.StoredItemTransactions;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.InventorySlotBinding;
import com.hbm.inventory.SlotSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import techreborn.blockentity.storage.item.StorageUnitBaseBlockEntity;
import techreborn.init.TRContent;

@Mixin(value = StorageUnitBaseBlockEntity.class, remap = false, priority = 500)
public abstract class MixinStorageUnitBaseBlockEntity implements SlotSource {
    @Shadow private ItemStack storeItemStack;
    @Shadow private TRContent.StorageUnit type;

    @Override
    public int storedSlotCount() {
        return type == null || type == TRContent.StorageUnit.CREATIVE || storeItemStack == null
                ? 0
                : 1;
    }

    @Override
    public ItemStack storedItem(int slot) {
        return storeItemStack;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (storeItemStack != expected || !StoredItemTransactions.isIdle()) return false;
        InventorySlotBinding.unbind(storeItemStack, this, 0);
        storeItemStack = replacement;
        InventoryChanges.publishedStack(this, 0, replacement);
        return true;
    }

    @Override
    public void publishStoredChanges(int slot) {
        MachineBaseBlockEntity owner = (MachineBaseBlockEntity) (Object) this;
        owner.setChanged();
        owner.syncWithAll();
    }
}

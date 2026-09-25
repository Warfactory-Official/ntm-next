// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin.storage;

import com.hbm.fabric.integration.StoredItemTransactions;
import com.hbm.interfaces.StoredItemSlots;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import techreborn.init.TRContent;

@Pseudo
@Mixin(targets = "techreborn.blockentity.storage.item.StorageUnitBaseBlockEntity", remap = false)
public abstract class MixinRebornStorageUnit implements StoredItemSlots {
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
        storeItemStack = replacement;
        MachineBaseBlockEntity owner = (MachineBaseBlockEntity) (Object) this;
        owner.setChanged();
        owner.syncWithAll();
        return true;
    }
}

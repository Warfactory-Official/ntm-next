// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.TrackedStorage;
import java.util.function.Consumer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = StorageBlockEntity.class, remap = false, priority = 500)
public abstract class MixinStorageBlockEntity implements StoredItems, TrackedStorage {
    @Shadow @Final private StorageWrapper storageWrapper;
    @Unique private Object hbm$inventory;
    @Unique private Object hbm$upgrades;

    @Override
    public void inventorySources(Consumer<Object> sources) {
        hbm$inventory = storageWrapper.getInventoryHandler();
        hbm$upgrades = storageWrapper.getUpgradeHandler();
        sources.accept(hbm$inventory);
        sources.accept(hbm$upgrades);
    }

    @Override
    public void refreshInventorySources() {
        if (hbm$inventory != storageWrapper.getInventoryHandler()
                || hbm$upgrades != storageWrapper.getUpgradeHandler())
            InventoryChanges.storageLayoutChanged((BlockEntity) (Object) this);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        visitor.visit((StoredItemSlots) storageWrapper.getInventoryHandler());
    }
}

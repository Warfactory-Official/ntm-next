// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.TrackedStorage;
import java.util.function.Consumer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BackpackBlockEntity.class, remap = false, priority = 500)
public abstract class MixinBackpackBlockEntity implements StoredItems, TrackedStorage {
    @Shadow private IBackpackWrapper backpackWrapper;
    @Unique private Object hbm$inventory;
    @Unique private Object hbm$upgrades;

    @Override
    public void inventorySources(Consumer<Object> sources) {
        hbm$inventory = hbm$upgrades = null;
        if (LinkedStorageStackLifecycle.classifyEndpoint(backpackWrapper.getBackpack())
                == LinkedStorageEndpointStackState.ENDPOINT) return;
        hbm$inventory = backpackWrapper.getInventoryHandler();
        hbm$upgrades = backpackWrapper.getUpgradeHandler();
        sources.accept(hbm$inventory);
        sources.accept(hbm$upgrades);
    }

    @Override
    public void refreshInventorySources() {
        if (LinkedStorageStackLifecycle.classifyEndpoint(backpackWrapper.getBackpack())
                == LinkedStorageEndpointStackState.ENDPOINT) {
            if (hbm$inventory != null)
                InventoryChanges.storageLayoutChanged((BlockEntity) (Object) this);
            return;
        }
        if (hbm$inventory != backpackWrapper.getInventoryHandler()
                || hbm$upgrades != backpackWrapper.getUpgradeHandler())
            InventoryChanges.storageLayoutChanged((BlockEntity) (Object) this);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (LinkedStorageStackLifecycle.classifyEndpoint(backpackWrapper.getBackpack())
                == LinkedStorageEndpointStackState.ENDPOINT) return;
        visitor.visit((StoredItemSlots) backpackWrapper.getInventoryHandler());
    }
}

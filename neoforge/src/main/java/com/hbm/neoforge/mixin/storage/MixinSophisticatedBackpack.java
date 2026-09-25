// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity", remap = false)
public abstract class MixinSophisticatedBackpack implements StoredItems {
    @Shadow private IBackpackWrapper backpackWrapper;

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (LinkedStorageStackLifecycle.classifyEndpoint(backpackWrapper.getBackpack())
                == LinkedStorageEndpointStackState.ENDPOINT) return;
        visitor.visit((StoredItemSlots) backpackWrapper.getInventoryHandler());
    }
}

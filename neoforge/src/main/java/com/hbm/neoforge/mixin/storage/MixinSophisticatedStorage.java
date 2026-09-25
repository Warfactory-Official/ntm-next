// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity", remap = false)
public abstract class MixinSophisticatedStorage implements StoredItems {
    @Shadow @Final private StorageWrapper storageWrapper;

    @Override
    public void visitStoredItems(Visitor visitor) {
        visitor.visit((StoredItemSlots) storageWrapper.getInventoryHandler());
    }
}

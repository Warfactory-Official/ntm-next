/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.neoforge.mixin.storage;

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
        visitor.visit(
                ((com.hbm.interfaces.StoredItemSlots) (storageWrapper.getInventoryHandler())));
    }
}

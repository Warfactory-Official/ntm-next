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

package com.hbm.inventory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public abstract class ItemInteriorBackend implements InventoryLocation {
    public final MinecraftServer server;
    private @Nullable Object selected;

    protected ItemInteriorBackend(MinecraftServer server) {
        this.server = server;
    }

    public abstract SlotSource[] inventorySources();

    public abstract void changed();

    public abstract boolean physicalSlot(SlotSource source, int slot);

    public abstract boolean replaceStoredItem(
            SlotSource source, int slot, ItemStack expected, ItemStack replacement);

    public abstract void publishStoredChanges(SlotSource source, int slot);

    public void slotChanged(SlotSource source, int slot) {}

    final void carrier(@Nullable Object selected) {
        this.selected = selected;
    }

    @Override
    public final @Nullable Object inventoryCarrier() {
        return selected;
    }
}

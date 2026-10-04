// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

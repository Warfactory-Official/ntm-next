// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.storage;

import com.hbm.inventory.SlotSource;
import com.tom.storagemod.util.FilingCabinetContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = FilingCabinetContainer.class, remap = false)
public abstract class MixinFilingCabinetContainer implements SlotSource {
    @Shadow private SimpleContainer inv;
    @Unique private @Nullable Object hbm$owner;

    @Override
    public @Nullable Object inventoryOwner() {
        return hbm$owner;
    }

    @Override
    public void inventoryOwner(@Nullable Object owner) {
        hbm$owner = owner;
    }

    @Override
    public void bindInventorySlots() {
        inv.hbm$trackSlots(this);
    }

    @Override
    public int storedSlotCount() {
        return inv.getContainerSize();
    }

    @Override
    public ItemStack storedItem(int slot) {
        return inv.getItem(slot);
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (inv.getItem(slot) != expected) return false;
        ((Container) (Object) this).setItem(slot, replacement);
        ((Container) (Object) this).setChanged();
        return true;
    }
}

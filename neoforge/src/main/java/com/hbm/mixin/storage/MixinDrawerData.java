// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.storage;

import com.hbm.inventory.SlotSource;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.StandardDrawerGroup.DrawerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = DrawerData.class, remap = false, priority = 500)
public abstract class MixinDrawerData implements SlotSource {
    @Shadow private ItemStack protoStack;
    @Shadow private int count;
    @Shadow private boolean missing;

    @Shadow
    protected abstract void setStoredItemCount(int count, boolean notify);

    @Shadow
    protected abstract void onItemChanged();

    @Shadow
    protected abstract void onAmountChanged();

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
    public int storedSlotCount() {
        return 1;
    }

    @Override
    public ItemStack storedItem(int slot) {
        return protoStack;
    }

    @Override
    public boolean physicalSlot(int slot) {
        if (slot != 0 || count <= 0 || missing) return false;
        boolean vending = __asm__(boolean) {
            aload this;
            checkcast "com/jaquadro/minecraft/storagedrawers/block/tile/tiledata/StandardDrawerGroup$DrawerData";
            invokevirtual "com/jaquadro/minecraft/storagedrawers/block/tile/tiledata/StandardDrawerGroup$DrawerData" "getAttributes" "()Lcom/jaquadro/minecraft/storagedrawers/api/storage/IDrawerAttributes;";
            invokeinterface "com/jaquadro/minecraft/storagedrawers/api/storage/IDrawerAttributes" "isUnlimitedVending" "()Z";
        };
        return !vending;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (protoStack != expected || !physicalSlot(slot)) return false;
        int amount = count;
        __asm__ {
            aload this;
            checkcast "com/jaquadro/minecraft/storagedrawers/block/tile/tiledata/StandardDrawerGroup$DrawerData";
            aload replacement;
            iconst_0;
            invokevirtual "com/jaquadro/minecraft/storagedrawers/block/tile/tiledata/StandardDrawerGroup$DrawerData" "setStoredItem" "(Lnet/minecraft/world/item/ItemStack;Z)Lcom/jaquadro/minecraft/storagedrawers/api/storage/IDrawer;";
            pop;
        }
        if (!replacement.isEmpty()) setStoredItemCount(amount, false);
        return replacement.isEmpty() ? count == 0 : ItemStack.matches(protoStack, replacement);
    }

    @Override
    public void publishStoredChanges(int slot) {
        onItemChanged();
        onAmountChanged();
    }
}

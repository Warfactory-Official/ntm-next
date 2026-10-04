// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.storage;

import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.InventorySlotBinding;
import com.hbm.inventory.SlotSource;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData",
        remap = false)
public abstract class MixinUpgradeData implements SlotSource {
    @Shadow @Final protected ItemStack[] upgrades;

    @Shadow
    public abstract boolean setUpgrade(int slot, ItemStack stack);

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
        return upgrades.length;
    }

    @Override
    public ItemStack storedItem(int slot) {
        return upgrades[slot];
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        return upgrades[slot] == expected && setUpgrade(slot, replacement);
    }

    @WrapMethod(method = "setUpgrade")
    private boolean hbm$upgradeSlotChanged(int slot, ItemStack stack, Operation<Boolean> original) {
        int index = Math.clamp(slot, 0, upgrades.length - 1);
        ItemStack previous = upgrades[index];
        boolean accepted = original.call(slot, stack);
        if (previous != upgrades[index]) {
            InventorySlotBinding.unbind(previous, this, index);
            InventoryChanges.publishedStack(this, index, upgrades[index]);
        }
        return accepted;
    }

    @Inject(
            method = {"read", "unbindRemoteUpgrade", "updateRemoteUpgradeBinding"},
            at = @At("RETURN"))
    private void hbm$reboundUpgradeArray(CallbackInfo ci) {
        bindInventorySlots();
        InventoryChanges.layoutChanged(this);
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import appeng.api.stacks.AEKey;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.InventoryOwnerAccess;
import com.hbm.neoforge.inventory.AE2CpuAnchor;
import com.hbm.neoforge.inventory.AE2CraftingSlotSource;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "appeng.crafting.execution.CraftingCpuLogic", remap = false)
public abstract class MixinCraftingCpuLogic implements AE2CraftingSlotSource {
    @Shadow @Final private ListCraftingInventory inventory;
    @Shadow @Final CraftingCPUCluster cluster;

    @Override
    public ListCraftingInventory craftingInventory() {
        return inventory;
    }

    @Override
    public boolean craftingInventoryResident() {
        Object owner = inventoryOwner();
        return owner != null
                && !cluster.isDestroyed()
                && ((AE2CpuAnchor) (Object) cluster).craftingInventoryAnchor() == owner
                && InventoryOwnerAccess.of(owner).live(owner);
    }

    @Override
    public void publishCraftingInventory() {
        if (craftingInventoryResident()) cluster.markDirty();
    }

    @Inject(method = "postChange", at = @At("RETURN"))
    private void hbm$keyDelta(AEKey key, CallbackInfo ci) {
        craftingKeyChanged(key);
    }

    @Inject(
            method = "storeItems",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lit/unimi/dsi/fastutil/objects/Object2LongMap$Entry;setValue(J)J",
                            shift = At.Shift.AFTER))
    private void hbm$storedFinalAmount(CallbackInfo ci, @Local Object2LongMap.Entry<AEKey> entry) {
        craftingKeyChanged(entry.getKey());
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void hbm$loadedHeldInventory(ValueInput input, CallbackInfo ci) {
        if (inventoryOwner() != null) {
            bindInventorySlots();
            InventoryChanges.layoutChanged(this);
        }
    }
}

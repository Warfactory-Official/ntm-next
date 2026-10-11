// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.CraftingSubmitResult;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.InventoryOwnerAccess;
import com.hbm.neoforge.inventory.AE2CpuAnchor;
import com.hbm.neoforge.inventory.AE2CraftingSlotSource;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingCpuLogic.class, remap = false, priority = 500)
public abstract class MixinCraftingCpuLogic implements AE2CraftingSlotSource {
    @Shadow @Final private ListCraftingInventory inventory;
    @Shadow @Final CraftingCPUCluster cluster;

    @Shadow
    public abstract void storeItems();

    @Inject(
            method = "trySubmitJob",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/crafting/execution/CraftingCpuHelper;tryExtractInitialItems(Lappeng/api/networking/crafting/ICraftingPlan;Lappeng/api/networking/IGrid;Lappeng/crafting/inv/ListCraftingInventory;Lappeng/api/networking/security/IActionSource;)Lappeng/api/stacks/GenericStack;"),
            cancellable = true)
    private void hbm$pendingReturns(
            IGrid grid,
            ICraftingPlan plan,
            IActionSource source,
            @Nullable ICraftingRequester requester,
            CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        if (!inventory.list.isEmpty()) cir.setReturnValue(CraftingSubmitResult.CPU_BUSY);
    }

    @Redirect(
            method = "trySubmitJob",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/crafting/execution/CraftingCpuHelper;tryExtractInitialItems(Lappeng/api/networking/crafting/ICraftingPlan;Lappeng/api/networking/IGrid;Lappeng/crafting/inv/ListCraftingInventory;Lappeng/api/networking/security/IActionSource;)Lappeng/api/stacks/GenericStack;"))
    private @Nullable GenericStack hbm$extractInitialItems(
            ICraftingPlan plan,
            IGrid grid,
            ListCraftingInventory cpuInventory,
            IActionSource source) {
        var storage = grid.getStorageService().getInventory();
        for (var entry : plan.usedItems()) {
            long requested = entry.getLongValue();
            long extracted =
                    storage.extract(entry.getKey(), requested, Actionable.MODULATE, source);
            cpuInventory.insert(entry.getKey(), extracted, Actionable.MODULATE);
            if (extracted < requested) {
                storeItems();
                return new GenericStack(entry.getKey(), requested - extracted);
            }
        }
        return null;
    }

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

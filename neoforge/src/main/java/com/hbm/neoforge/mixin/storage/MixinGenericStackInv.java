// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import com.hbm.inventory.ArraySlotSource;
import com.hbm.inventory.InventoryChanges;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GenericStackInv.class, remap = false, priority = 500)
public abstract class MixinGenericStackInv implements ArraySlotSource {
    @Shadow @Final protected GenericStack[] stacks;
    @Shadow @Final protected GenericStackInv.Mode mode;

    @Shadow
    public abstract void onChange();

    @Override
    public int storedSlotCount() {
        return stacks.length;
    }

    @Override
    public void bindInventorySlots() {}

    @Override
    public boolean physicalSlot(int slot) {
        return mode == GenericStackInv.Mode.STORAGE
                && slot >= 0
                && slot < stacks.length
                && stacks[slot] != null
                && stacks[slot].amount() > 0
                && stacks[slot].what() instanceof AEItemKey;
    }

    @Override
    public ItemStack storedItem(int slot) {
        GenericStack held = stacks[slot];
        return held != null && held.amount() > 0 && held.what() instanceof AEItemKey item
                ? item.getReadOnlyStack()
                : ItemStack.EMPTY;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || !physicalSlot(slot)
                || storedItem(slot) != expected) return false;
        assert replacement.isEmpty() || replacement.getCount() == expected.getCount();
        GenericStack held = stacks[slot];
        stacks[slot] =
                replacement.isEmpty()
                        ? null
                        : new GenericStack(AEItemKey.of(replacement), held.amount());
        InventoryChanges.slotChanged(this, slot);
        return true;
    }

    @Override
    public void publishStoredChanges(int slot) {
        onChange();
    }

    @Inject(method = "revertToSnapshot([Lappeng/api/stacks/GenericStack;)V", at = @At("HEAD"))
    private void hbm$rollbackSlots(GenericStack[] snapshot, CallbackInfo ci) {
        for (int slot = 0; slot < stacks.length; slot++)
            if (!Objects.equals(stacks[slot], snapshot[slot]))
                InventoryChanges.slotChanged(this, slot);
    }
}

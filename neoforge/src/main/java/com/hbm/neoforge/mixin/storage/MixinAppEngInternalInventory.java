// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.inventory.ListSlotSource;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "appeng.util.inv.AppEngInternalInventory", remap = false)
public abstract class MixinAppEngInternalInventory implements ListSlotSource {
    @Shadow @Final @Mutable private NonNullList<ItemStack> stacks;
    @Shadow private boolean inTransactionalCode;

    @Shadow
    public abstract void setItemDirect(int slot, ItemStack stack);

    @Override
    public boolean directStoredMutation() {
        return false;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (inTransactionalCode
                || Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || stacks.get(slot) != expected) return false;
        setItemDirect(slot, replacement);
        return true;
    }
}

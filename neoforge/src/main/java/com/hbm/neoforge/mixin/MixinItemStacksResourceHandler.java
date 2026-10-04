// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin;

import com.hbm.inventory.ListSlotSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStacksResourceHandler.class)
public abstract class MixinItemStacksResourceHandler
        extends StacksResourceHandler<ItemStack, ItemResource> implements ListSlotSource {
    protected MixinItemStacksResourceHandler() {
        super(0, ItemStack.EMPTY, ItemStack.OPTIONAL_CODEC);
    }

    @Override
    public boolean directStoredMutation() {
        return false;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || stacks.get(slot) != expected) return false;
        ((ItemStacksResourceHandler) (Object) this)
                .set(slot, ItemResource.of(replacement), replacement.getCount());
        return ItemStack.matches(stacks.get(slot), replacement);
    }
}

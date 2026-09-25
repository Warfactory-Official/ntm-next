// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface StoredItems {
    void visitStoredItems(Visitor visitor);

    interface Visitor {
        boolean visit(ItemStack stack);

        boolean visit(Container container);

        boolean visit(BlockEntity blockEntity);

        boolean visit(StoredItemSlots slots);

        boolean visitOwned(Container container);

        void afterChanges(Runnable notification);
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.network.BlockEntityCraneInserter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuCraneInserter extends BlockEntityMenu<BlockEntityCraneInserter> {

    public MenuCraneInserter(
            int containerId, Inventory playerInventory, BlockEntityCraneInserter be) {
        super(ModMenus.CRANE_INSERTER.get(), containerId, be);
        checkContainerSize(be, BlockEntityCraneInserter.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                addSlot(new Slot(be, j + i * 7, 8 + j * 18, 17 + i * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityCraneInserter.SLOT_COUNT);
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.network.BlockEntityCraneBoxer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuCraneBoxer extends BlockEntityMenu<BlockEntityCraneBoxer> {

    public MenuCraneBoxer(int containerId, Inventory playerInventory, BlockEntityCraneBoxer be) {
        super(ModMenus.CRANE_BOXER.get(), containerId, be);
        checkContainerSize(be, BlockEntityCraneBoxer.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                addSlot(new Slot(be, j + i * 7, 8 + j * 18, 17 + i * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityCraneBoxer.SLOT_COUNT);
    }
}

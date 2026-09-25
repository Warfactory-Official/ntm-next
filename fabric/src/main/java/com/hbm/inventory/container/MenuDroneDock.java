// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.network.BlockEntityDroneDock;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuDroneDock extends BlockEntityMenu<BlockEntityDroneDock> {

    public MenuDroneDock(int containerId, Inventory playerInventory, BlockEntityDroneDock dock) {
        super(ModMenus.DRONE_DOCK.get(), containerId, dock);
        checkContainerSize(dock, BlockEntityDroneDock.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(dock, col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveFilteredUnsorted(player, index, BlockEntityDroneDock.SLOT_COUNT);
    }
}

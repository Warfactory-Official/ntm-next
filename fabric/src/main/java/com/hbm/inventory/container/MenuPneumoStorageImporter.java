// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageImporter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuPneumoStorageImporter extends BlockEntityMenu<BlockEntityPneumoStorageImporter> {

    public MenuPneumoStorageImporter(
            int containerId, Inventory playerInventory, BlockEntityPneumoStorageImporter be) {
        super(ModMenus.PNEUMATIC_STORAGE_IMPORTER.get(), containerId, be);
        checkContainerSize(be, BlockEntityPneumoStorageImporter.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(be, col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityPneumoStorageImporter.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackToFiltered(stack, machineEnd, slots.size(), true);
                    return moveItemStackToFiltered(stack, 0, machineEnd, false) || false;
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityStorageDrum;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuStorageDrum extends BlockEntityMenu<BlockEntityStorageDrum> {

    public MenuStorageDrum(int containerId, Inventory playerInv, BlockEntityStorageDrum be) {
        super(ModMenus.STORAGE_DRUM.get(), containerId, be);
        checkContainerSize(be, BlockEntityStorageDrum.SLOT_COUNT);

        int index = 0;
        for (int j = 0; j < 6; j++) {
            for (int i = 0; i < 6; i++) {
                if (i + j > 1 && i + j < 9 && 5 - i + j > 1 && i + 5 - j > 1) {
                    addSlot(new Slot(be, index, 35 + i * 18, 24 + j * 18));
                    index++;
                }
            }
        }

        addStandardInventorySlots(playerInv, 8, 152);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityStorageDrum.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}

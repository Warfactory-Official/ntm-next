// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageClutter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuPneumoStorageClutter extends BlockEntityMenu<BlockEntityPneumoStorageClutter> {

    public MenuPneumoStorageClutter(
            int containerId, Inventory playerInventory, BlockEntityPneumoStorageClutter be) {
        super(ModMenus.PNEUMATIC_STORAGE_CLUTTER.get(), containerId, be);
        checkContainerSize(be, BlockEntityPneumoStorageClutter.SLOT_COUNT);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new ShulkerBoxSlot(be, col + row * 9, 8 + col * 18, 17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 153);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityPneumoStorageClutter.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackToFiltered(stack, machineEnd, slots.size(), true);
                    return moveItemStackToFiltered(stack, 0, machineEnd, false) || false;
                });
    }
}

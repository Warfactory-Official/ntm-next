// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.storage.BlockEntityBarrel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuBarrel extends BlockEntityMenu<BlockEntityBarrel> {

    public MenuBarrel(int containerId, Inventory playerInv, BlockEntityBarrel barrel) {
        this(containerId, playerInv, barrel, barrel);
    }

    private MenuBarrel(
            int containerId, Inventory playerInv, Container container, BlockEntityBarrel barrel) {
        super(ModMenus.BARREL.get(), containerId, barrel, container);
        checkContainerSize(container, BlockEntityBarrel.SLOT_COUNT);

        addSlot(new Slot(container, 0, 8, 17));
        addSlot(new Slot(container, 1, 8, 53));
        addSlot(new Slot(container, 2, 35, 17));
        addSlot(new Slot(container, 3, 35, 53));
        addSlot(new Slot(container, 4, 125, 17));
        addSlot(new Slot(container, 5, 125, 53));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityBarrel.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}

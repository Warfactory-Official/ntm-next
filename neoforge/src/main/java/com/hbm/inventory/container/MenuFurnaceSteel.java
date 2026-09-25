// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotSmelting;
import com.hbm.tileentity.machine.BlockEntityFurnaceSteel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuFurnaceSteel extends BlockEntityMenu<BlockEntityFurnaceSteel> {

    public MenuFurnaceSteel(int containerId, Inventory playerInv, BlockEntityFurnaceSteel be) {
        this(containerId, playerInv, be, be);
    }

    private MenuFurnaceSteel(
            int containerId, Inventory playerInv, Container container, BlockEntityFurnaceSteel be) {
        super(ModMenus.FURNACE_STEEL.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityFurnaceSteel.SLOT_COUNT);

        addSlot(new Slot(container, 0, 35, 17));
        addSlot(new Slot(container, 1, 35, 35));
        addSlot(new Slot(container, 2, 35, 53));
        addSlot(new SlotSmelting(playerInv.player, be, 3, 125, 17));
        addSlot(new SlotSmelting(playerInv.player, be, 4, 125, 35));
        addSlot(new SlotSmelting(playerInv.player, be, 5, 125, 53));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityFurnaceSteel.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    return moveItemStackTo(stack, 0, 3, false);
                });
    }
}

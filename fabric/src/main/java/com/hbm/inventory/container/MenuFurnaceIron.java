// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotSmelting;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityFurnaceIron;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuFurnaceIron extends BlockEntityMenu<BlockEntityFurnaceIron> {

    public MenuFurnaceIron(int containerId, Inventory playerInv, BlockEntityFurnaceIron be) {
        this(containerId, playerInv, be, be);
    }

    private MenuFurnaceIron(
            int containerId, Inventory playerInv, Container container, BlockEntityFurnaceIron be) {
        super(ModMenus.FURNACE_IRON.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityFurnaceIron.SLOT_COUNT);

        addSlot(new Slot(container, BlockEntityFurnaceIron.SLOT_INPUT, 53, 17));
        addSlot(new Slot(container, BlockEntityFurnaceIron.SLOT_FUEL_A, 53, 53));
        addSlot(new Slot(container, BlockEntityFurnaceIron.SLOT_FUEL_B, 71, 53));
        addSlot(
                new SlotSmelting(
                        playerInv.player, be, BlockEntityFurnaceIron.SLOT_OUTPUT, 125, 35));
        addSlot(new SlotUpgrade(container, BlockEntityFurnaceIron.SLOT_UPGRADE, 17, 35));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityFurnaceIron.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityFurnaceIron.SLOT_UPGRADE,
                                BlockEntityFurnaceIron.SLOT_UPGRADE + 1,
                                false);
                    if (container().canPlaceItem(BlockEntityFurnaceIron.SLOT_FUEL_A, stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityFurnaceIron.SLOT_FUEL_A,
                                BlockEntityFurnaceIron.SLOT_FUEL_B + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityFurnaceIron.SLOT_INPUT,
                            BlockEntityFurnaceIron.SLOT_INPUT + 1,
                            false);
                });
    }
}

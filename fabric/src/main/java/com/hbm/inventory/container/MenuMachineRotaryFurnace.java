// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.handler.FuelHandler;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityMachineRotaryFurnace;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineRotaryFurnace extends BlockEntityMenu<BlockEntityMachineRotaryFurnace> {

    public MenuMachineRotaryFurnace(
            int containerId, Inventory playerInv, BlockEntityMachineRotaryFurnace be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineRotaryFurnace(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineRotaryFurnace be) {
        super(ModMenus.MACHINE_ROTARY_FURNACE.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineRotaryFurnace.SLOT_COUNT);

        addSlot(new Slot(container, 0, 8, 18));
        addSlot(new Slot(container, 1, 26, 18));
        addSlot(new Slot(container, 2, 44, 18));

        addSlot(
                new SlotFiltered(
                        container,
                        3,
                        8,
                        54,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addSlot(new Slot(container, 4, 44, 54));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineRotaryFurnace.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (FuelHandler.getBurnTime(player.level(), stack) > 0)
                        return moveItemStackTo(stack, 4, 5, false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(stack, 3, 4, false);
                    return moveItemStackTo(stack, 0, 3, false);
                });
    }
}

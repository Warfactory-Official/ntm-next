// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntitySILEX;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineSILEX extends BlockEntityMenu<BlockEntitySILEX> {

    public MenuMachineSILEX(int containerId, Inventory playerInv, BlockEntitySILEX be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineSILEX(
            int containerId, Inventory playerInv, Container container, BlockEntitySILEX be) {
        super(ModMenus.MACHINE_SILEX.get(), containerId, be, container);
        checkContainerSize(container, BlockEntitySILEX.SLOT_COUNT);

        addSlot(new Slot(container, BlockEntitySILEX.SLOT_INPUT, 80, 12));
        addSlot(new Slot(container, BlockEntitySILEX.SLOT_FLUID_ID, 8, 24));
        addSlot(new Slot(container, BlockEntitySILEX.SLOT_CONTAINER_IN, 26, 24));
        addSlot(
                new SlotFiltered(
                        container, BlockEntitySILEX.SLOT_CONTAINER_OUT, 44, 24, SlotFiltered.NONE));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, container, BlockEntitySILEX.SLOT_OUTPUT, 116, 90));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 5, 134, 72));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 6, 152, 72));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 7, 134, 90));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 8, 152, 90));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 9, 134, 108));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 10, 152, 108));

        addStandardInventorySlots(playerInv, 8, 140);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntitySILEX.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntitySILEX.SLOT_FLUID_ID,
                                BlockEntitySILEX.SLOT_FLUID_ID + 1,
                                false);
                    if (blockEntity().tank.containerContent(stack) > 0) {
                        return moveItemStackTo(
                                stack,
                                BlockEntitySILEX.SLOT_CONTAINER_IN,
                                BlockEntitySILEX.SLOT_CONTAINER_IN + 1,
                                false);
                    }
                    return moveItemStackTo(
                            stack,
                            BlockEntitySILEX.SLOT_INPUT,
                            BlockEntitySILEX.SLOT_INPUT + 1,
                            false);
                });
    }
}

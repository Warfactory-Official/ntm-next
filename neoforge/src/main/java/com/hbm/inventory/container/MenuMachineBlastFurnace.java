// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.handler.FuelHandler;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.BlockEntityMachineBlastFurnace;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineBlastFurnace extends BlockEntityMenu<BlockEntityMachineBlastFurnace> {

    public MenuMachineBlastFurnace(
            int containerId, Inventory playerInv, BlockEntityMachineBlastFurnace be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineBlastFurnace(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineBlastFurnace be) {
        super(ModMenus.MACHINE_BLAST_FURNACE.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineBlastFurnace.SLOT_COUNT);

        addSlot(SlotFiltered.gated(container, BlockEntityMachineBlastFurnace.SLOT_FUEL, 80, 81));
        addSlot(new Slot(container, BlockEntityMachineBlastFurnace.SLOT_INPUT_1, 80, 27));
        addSlot(new Slot(container, BlockEntityMachineBlastFurnace.SLOT_INPUT_2, 80, 45));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineBlastFurnace.SLOT_OUTPUT_1,
                        134,
                        72));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineBlastFurnace.SLOT_OUTPUT_2,
                        134,
                        90));

        addStandardInventorySlots(playerInv, 8, 140);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineBlastFurnace.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd)
                        return moveItemStackToFiltered(stack, machineEnd, invEnd, true);
                    if (FuelHandler.getBurnTime(player.level(), stack) > 0)
                        return moveItemStackToFiltered(
                                stack,
                                BlockEntityMachineBlastFurnace.SLOT_FUEL,
                                BlockEntityMachineBlastFurnace.SLOT_FUEL + 1,
                                false);
                    return moveItemStackToFiltered(
                            stack,
                            BlockEntityMachineBlastFurnace.SLOT_INPUT_1,
                            BlockEntityMachineBlastFurnace.SLOT_INPUT_2 + 1,
                            false);
                });
    }
}

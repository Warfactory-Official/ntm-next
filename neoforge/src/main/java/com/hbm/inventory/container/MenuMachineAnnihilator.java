// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityMachineAnnihilator;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineAnnihilator extends BlockEntityMenu<BlockEntityMachineAnnihilator> {

    public MenuMachineAnnihilator(
            int containerId, Inventory playerInv, BlockEntityMachineAnnihilator be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineAnnihilator(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineAnnihilator be) {
        super(ModMenus.MACHINE_ANNIHILATOR.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineAnnihilator.SLOT_COUNT);

        addSlot(new Slot(container, BlockEntityMachineAnnihilator.SLOT_TRASH, 17, 45));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineAnnihilator.SLOT_FLUID_ID,
                        35,
                        45,
                        s -> s.getItem() instanceof FluidIdentifierItem));
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = BlockEntityMachineAnnihilator.SLOT_PAYOUT_START + col + row * 3;
                addSlot(
                        new SlotFiltered(
                                container, index, 80 + col * 18, 36 + row * 18, SlotFiltered.NONE));
            }
        }
        addSlot(new Slot(container, BlockEntityMachineAnnihilator.SLOT_MONITOR, 152, 18));
        addSlot(new Slot(container, BlockEntityMachineAnnihilator.SLOT_PAYOUT_REQUEST, 152, 62));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineAnnihilator.SLOT_PAYOUT_RESULT,
                        152,
                        80));

        addStandardInventorySlots(playerInv, 8, 126);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineAnnihilator.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineAnnihilator.SLOT_FLUID_ID,
                                BlockEntityMachineAnnihilator.SLOT_FLUID_ID + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineAnnihilator.SLOT_TRASH,
                            BlockEntityMachineAnnihilator.SLOT_TRASH + 1,
                            false);
                });
    }
}

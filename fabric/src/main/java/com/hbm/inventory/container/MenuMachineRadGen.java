// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.BlockEntityMachineRadGen;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineRadGen extends BlockEntityMenu<BlockEntityMachineRadGen> {

    private static final int COLUMNS = 3;
    private static final int ROWS = 4;

    public MenuMachineRadGen(int containerId, Inventory playerInv, BlockEntityMachineRadGen be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineRadGen(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineRadGen be) {
        super(ModMenus.MACHINE_RADGEN.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineRadGen.SLOT_COUNT);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slot = col + row * COLUMNS;
                addSlot(
                        new SlotFiltered(
                                container,
                                slot,
                                8 + col * 18,
                                17 + row * 18,
                                stack -> be.canPlaceItem(slot, stack)));
            }
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player,
                                container,
                                col + row * COLUMNS + BlockEntityMachineRadGen.CHANNELS,
                                116 + col * 18,
                                17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInv, 8, 102);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < BlockEntityMachineRadGen.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, BlockEntityMachineRadGen.SLOT_COUNT, slots.size(), true);
                    }
                    return moveItemStackTo(stack, 0, BlockEntityMachineRadGen.CHANNELS, false);
                });
    }
}

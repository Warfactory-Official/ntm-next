// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.ItemMold;
import com.hbm.tileentity.machine.BlockEntityMachineStrandCaster;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineStrandCaster extends BlockEntityMenu<BlockEntityMachineStrandCaster> {

    public MenuMachineStrandCaster(
            int containerId, Inventory playerInv, BlockEntityMachineStrandCaster be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineStrandCaster(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineStrandCaster be) {
        super(ModMenus.MACHINE_STRAND_CASTER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineStrandCaster.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineStrandCaster.SLOT_MOLD,
                        57,
                        62,
                        s -> s.getItem() instanceof ItemMold));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player,
                                container,
                                BlockEntityMachineStrandCaster.SLOT_OUTPUT_START + i * 2 + j,
                                125 + j * 18,
                                26 + i * 18));
            }
        }

        addStandardInventorySlots(playerInv, 8, 132);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineStrandCaster.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);

                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineStrandCaster.SLOT_OUTPUT_START,
                            BlockEntityMachineStrandCaster.SLOT_OUTPUT_START + 1,
                            false);
                });
    }
}

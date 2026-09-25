// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.BlockEntityMachineFunnel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineFunnel extends BlockEntityMenu<BlockEntityMachineFunnel> {

    public MenuMachineFunnel(
            int containerId, Inventory playerInv, BlockEntityMachineFunnel funnel) {
        super(ModMenus.MACHINE_FUNNEL.get(), containerId, funnel);
        checkContainerSize(funnel, BlockEntityMachineFunnel.SLOT_COUNT);

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(funnel, i, 8 + 18 * i, 18));
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new SlotRecipeOutput(playerInv.player, funnel, i + 9, 8 + 18 * i, 54));
        }

        addStandardInventorySlots(playerInv, 8, 86);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineFunnel.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);

                    return moveItemStackTo(stack, 0, 9, false);
                });
    }
}

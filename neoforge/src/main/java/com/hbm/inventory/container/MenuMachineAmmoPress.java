// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.BlockEntityMachineAmmoPress;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineAmmoPress extends BlockEntityMenu<BlockEntityMachineAmmoPress> {

    public MenuMachineAmmoPress(
            int containerId, Inventory playerInv, BlockEntityMachineAmmoPress be) {
        super(ModMenus.MACHINE_AMMO_PRESS.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineAmmoPress.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int slot = i * 3 + j;
                addSlot(
                        new SlotFiltered(
                                be,
                                slot,
                                116 + j * 18,
                                18 + i * 18,
                                stack -> be.canPlaceItem(slot, stack)));
            }
        }
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityMachineAmmoPress.SLOT_OUTPUT, 134, 72));

        addStandardInventorySlots(playerInv, 8, 118);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineAmmoPress.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);

                    return moveItemStackTo(stack, 0, 9, false);
                });
    }
}

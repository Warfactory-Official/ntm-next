// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuRBMKOutgasser extends BlockEntityMenu<BlockEntityRBMKOutgasser> {

    public MenuRBMKOutgasser(int containerId, Inventory playerInv, BlockEntityRBMKOutgasser be) {
        this(containerId, playerInv, be, be);
    }

    private MenuRBMKOutgasser(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityRBMKOutgasser be) {
        super(ModMenus.RBMK_OUTGASSER.get(), containerId, be, container);
        checkContainerSize(container, 2);

        addSlot(new Slot(container, BlockEntityRBMKOutgasser.SLOT_INPUT, 48, 45));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityRBMKOutgasser.SLOT_OUTPUT,
                        112,
                        69));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = 2;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, 1, false);
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.BlockEntityAshpit;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineAshpit extends BlockEntityMenu<BlockEntityAshpit> {

    public MenuMachineAshpit(int containerId, Inventory playerInv, BlockEntityAshpit be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineAshpit(
            int containerId, Inventory playerInv, Container container, BlockEntityAshpit be) {
        super(ModMenus.MACHINE_ASHPIT.get(), containerId, be, container);
        checkContainerSize(container, 5);
        container.startOpen(playerInv.player);

        for (int i = 0; i < 5; i++) {
            addSlot(new SlotFiltered(container, i, 44 + i * 18, 27, SlotFiltered.NONE));
        }

        addStandardInventorySlots(playerInv, 8, 86);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index > 4) return false;
                    return moveItemStackTo(stack, 5, slots.size(), true);
                });
    }
}

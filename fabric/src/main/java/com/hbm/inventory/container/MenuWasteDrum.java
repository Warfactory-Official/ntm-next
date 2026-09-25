// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityWasteDrum;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuWasteDrum extends NtmContainerMenu {

    private static final int[] SLOTS = {
        71, 21, 89, 21, 53, 39, 71, 39, 89, 39, 107, 39, 53, 57, 71, 57, 89, 57, 107, 57, 71, 75,
        89, 75
    };

    public MenuWasteDrum(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(12));
    }

    public MenuWasteDrum(int containerId, Inventory playerInv, BlockEntityWasteDrum be) {
        this(containerId, playerInv, (Container) be);
    }

    private MenuWasteDrum(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.WASTE_DRUM.get(), containerId, container);
        checkContainerSize(container, 12);

        for (int i = 0; i < 12; i++) {
            addSlot(
                    new Slot(container, i, SLOTS[i * 2], SLOTS[i * 2 + 1]) {
                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }
                    });
        }

        addStandardInventorySlots(playerInv, 8, 107);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = 12;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}

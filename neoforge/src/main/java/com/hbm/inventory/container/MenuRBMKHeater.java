// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuRBMKHeater extends BlockEntityMenu<BlockEntityRBMKHeater> {

    public MenuRBMKHeater(int containerId, Inventory playerInv, BlockEntityRBMKHeater be) {
        this(containerId, playerInv, be, be);
    }

    private MenuRBMKHeater(
            int containerId, Inventory playerInv, Container container, BlockEntityRBMKHeater be) {
        super(ModMenus.RBMK_HEATER.get(), containerId, be, container);
        checkContainerSize(container, 1);

        addSlot(
                new Slot(container, 0, 41, 45) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof FluidIdentifierItem;
                    }
                });

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = 1;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}

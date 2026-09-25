// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityHeaterHeatex;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineHeatex extends BlockEntityMenu<BlockEntityHeaterHeatex> {

    public MenuMachineHeatex(int containerId, Inventory playerInv, BlockEntityHeaterHeatex be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineHeatex(
            int containerId, Inventory playerInv, Container container, BlockEntityHeaterHeatex be) {
        super(ModMenus.HEATER_HEATEX.get(), containerId, be, container);
        checkContainerSize(container, 1);
        container.startOpen(playerInv.player);

        addSlot(
                new SlotFiltered(
                        container,
                        0,
                        80,
                        72,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 122);
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
                    if (index == 0) return moveItemStackTo(stack, 1, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(stack, 0, 1, false);
                    return false;
                });
    }
}

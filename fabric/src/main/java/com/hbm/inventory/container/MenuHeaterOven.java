// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityHeaterOven;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuHeaterOven extends BlockEntityMenu<BlockEntityHeaterOven> {

    private static final int SLOT_COUNT = 2;

    public MenuHeaterOven(int containerId, Inventory playerInv, BlockEntityHeaterOven be) {
        this(containerId, playerInv, be, be);
    }

    private MenuHeaterOven(
            int containerId, Inventory playerInv, Container container, BlockEntityHeaterOven be) {
        super(ModMenus.HEATER_OVEN.get(), containerId, be, container);
        checkContainerSize(container, SLOT_COUNT);
        container.startOpen(playerInv.player);

        addSlot(new Slot(container, 0, 44, 27));
        addSlot(new Slot(container, 1, 62, 27));

        addStandardInventorySlots(playerInv, 8, 86);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, SLOT_COUNT);
    }
}

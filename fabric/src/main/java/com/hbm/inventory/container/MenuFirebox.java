// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityHeaterFirebox;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuFirebox extends BlockEntityMenu<BlockEntityHeaterFirebox> {

    private static final int SLOT_COUNT = 2;

    public MenuFirebox(int containerId, Inventory playerInv, BlockEntityHeaterFirebox be) {
        this(containerId, playerInv, be, be);
    }

    private MenuFirebox(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityHeaterFirebox be) {
        super(ModMenus.HEATER_FIREBOX.get(), containerId, be, container);
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

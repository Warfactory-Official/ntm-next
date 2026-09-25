// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuRBMKControlAuto extends BlockEntityMenu<BlockEntityRBMKControlAuto> {

    public MenuRBMKControlAuto(
            int containerId, Inventory playerInv, BlockEntityRBMKControlAuto be) {
        this(containerId, playerInv, be, be);
    }

    private MenuRBMKControlAuto(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityRBMKControlAuto be) {
        super(ModMenus.RBMK_CONTROL_AUTO.get(), containerId, be, container);
        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

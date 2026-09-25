// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuRBMKConsole extends BlockEntityMenu<BlockEntityRBMKConsole> {

    public MenuRBMKConsole(int containerId, Inventory playerInv, BlockEntityRBMKConsole console) {
        super(ModMenus.RBMK_CONSOLE.get(), containerId, console);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

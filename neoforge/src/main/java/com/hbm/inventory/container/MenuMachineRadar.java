// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineRadar extends BlockEntityMenu<BlockEntityMachineRadar> {

    public MenuMachineRadar(int containerId, Inventory playerInv, BlockEntityMachineRadar be) {
        super(ModMenus.MACHINE_RADAR.get(), containerId, be);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity().isRadarMenuValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

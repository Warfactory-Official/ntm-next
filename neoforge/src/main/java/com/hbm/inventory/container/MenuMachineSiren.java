// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityMachineSiren;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineSiren extends BlockEntityMenu<BlockEntityMachineSiren> {

    public MenuMachineSiren(int containerId, Inventory playerInv, BlockEntityMachineSiren be) {
        super(ModMenus.MACHINE_SIREN.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineSiren.SLOT_COUNT);

        addSlot(new Slot(be, 0, 8, 35));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityMachineSiren.SLOT_COUNT);
    }
}

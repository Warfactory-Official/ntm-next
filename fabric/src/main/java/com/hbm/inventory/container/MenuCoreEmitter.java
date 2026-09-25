// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityCoreEmitter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuCoreEmitter extends BlockEntityMenu<BlockEntityCoreEmitter> {

    public MenuCoreEmitter(int containerId, Inventory playerInv, BlockEntityCoreEmitter be) {
        super(ModMenus.CORE_EMITTER.get(), containerId, be);
        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

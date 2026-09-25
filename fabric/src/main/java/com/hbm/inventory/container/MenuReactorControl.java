// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.BlockEntityReactorControl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuReactorControl extends BlockEntityMenu<BlockEntityReactorControl> {

    public MenuReactorControl(int containerId, Inventory playerInv, BlockEntityReactorControl be) {
        super(ModMenus.REACTOR_CONTROL.get(), containerId, be, be);
        checkContainerSize(be, 1);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityReactorControl.SLOT_SENSOR,
                        92,
                        38,
                        stack -> stack.is(ModItems.REACTOR_SENSOR.get())));
        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, 1);
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityMachineSatLinker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineSatLinker extends BlockEntityMenu<BlockEntityMachineSatLinker> {

    public MenuMachineSatLinker(
            int containerId, Inventory playerInventory, BlockEntityMachineSatLinker be) {
        super(ModMenus.MACHINE_SATLINKER.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineSatLinker.SLOT_COUNT);

        addSlot(new Slot(be, BlockEntityMachineSatLinker.SLOT_SOURCE, 44, 36));
        addSlot(new Slot(be, BlockEntityMachineSatLinker.SLOT_TARGET, 80, 36));
        addSlot(new Slot(be, BlockEntityMachineSatLinker.SLOT_RANDOMIZE, 116, 36));

        addStandardInventorySlots(playerInventory, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index <= BlockEntityMachineSatLinker.SLOT_SOURCE
                                ? moveItemStackTo(
                                        stack,
                                        BlockEntityMachineSatLinker.SLOT_SOURCE + 1,
                                        slots.size(),
                                        true)
                                : moveItemStackTo(
                                        stack,
                                        BlockEntityMachineSatLinker.SLOT_SOURCE,
                                        BlockEntityMachineSatLinker.SLOT_SOURCE + 1,
                                        false));
    }
}

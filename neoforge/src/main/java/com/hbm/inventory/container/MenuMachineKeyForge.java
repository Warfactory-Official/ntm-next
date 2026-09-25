// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityMachineKeyForge;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineKeyForge extends BlockEntityMenu<BlockEntityMachineKeyForge> {

    public MenuMachineKeyForge(
            int containerId, Inventory playerInventory, BlockEntityMachineKeyForge blockEntity) {
        super(ModMenus.MACHINE_KEYFORGE.get(), containerId, blockEntity);
        checkContainerSize(blockEntity, BlockEntityMachineKeyForge.SLOT_COUNT);

        addSlot(new Slot(blockEntity, BlockEntityMachineKeyForge.SLOT_SOURCE, 44, 36));
        addSlot(new Slot(blockEntity, BlockEntityMachineKeyForge.SLOT_COPY, 80, 36));
        addSlot(new Slot(blockEntity, BlockEntityMachineKeyForge.SLOT_RANDOM, 116, 36));
        addStandardInventorySlots(playerInventory, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index == BlockEntityMachineKeyForge.SLOT_SOURCE)
                        return moveItemStackTo(
                                stack, BlockEntityMachineKeyForge.SLOT_COUNT, slots.size(), true);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineKeyForge.SLOT_SOURCE,
                            BlockEntityMachineKeyForge.SLOT_SOURCE + 1,
                            false);
                });
    }
}

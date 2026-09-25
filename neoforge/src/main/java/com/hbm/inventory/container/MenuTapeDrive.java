// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.ItemDrive;
import com.hbm.tileentity.machine.BlockEntityMachineTapeDrive;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuTapeDrive extends BlockEntityMenu<BlockEntityMachineTapeDrive> {
    public MenuTapeDrive(
            int containerId, Inventory playerInventory, BlockEntityMachineTapeDrive be) {
        super(ModMenus.MACHINE_TAPE_DRIVE.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineTapeDrive.SLOT_COUNT);
        for (int i = 0; i < BlockEntityMachineTapeDrive.SLOT_COUNT; i++) {
            addSlot(
                    new SlotFiltered(
                            be,
                            i,
                            35 + (i % 6) * 18,
                            27 + (i / 6) * 18,
                            stack -> stack.getItem() instanceof ItemDrive));
        }
        addStandardInventorySlots(playerInventory, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index < BlockEntityMachineTapeDrive.SLOT_COUNT
                                ? moveItemStackTo(
                                        stack,
                                        BlockEntityMachineTapeDrive.SLOT_COUNT,
                                        slots.size(),
                                        true)
                                : moveItemStackTo(
                                        stack, 0, BlockEntityMachineTapeDrive.SLOT_COUNT, false));
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.BlockEntityMicrowave;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineMicrowave extends BlockEntityMenu<BlockEntityMicrowave> {

    public MenuMachineMicrowave(int containerId, Inventory playerInv, BlockEntityMicrowave be) {
        super(ModMenus.MACHINE_MICROWAVE.get(), containerId, be);
        checkContainerSize(be, BlockEntityMicrowave.SLOT_COUNT);

        addSlot(new Slot(be, BlockEntityMicrowave.SLOT_INPUT, 80, 35));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityMicrowave.SLOT_OUTPUT, 140, 35));
        addSlot(
                new SlotFiltered(
                        be, BlockEntityMicrowave.SLOT_BATTERY, 8, 53, IBatteryItem::isBattery));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMicrowave.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMicrowave.SLOT_BATTERY,
                                BlockEntityMicrowave.SLOT_BATTERY + 1,
                                false);

                    return moveItemStackTo(
                                    stack,
                                    BlockEntityMicrowave.SLOT_INPUT,
                                    BlockEntityMicrowave.SLOT_INPUT + 1,
                                    false)
                            || moveItemStackTo(
                                    stack,
                                    BlockEntityMicrowave.SLOT_BATTERY,
                                    BlockEntityMicrowave.SLOT_BATTERY + 1,
                                    false);
                });
    }
}

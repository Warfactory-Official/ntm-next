// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.albion.BlockEntityPADetector;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuPADetector extends BlockEntityMenu<BlockEntityPADetector> {

    public MenuPADetector(int containerId, Inventory playerInv, BlockEntityPADetector be) {
        super(ModMenus.PA_DETECTOR.get(), containerId, be);
        checkContainerSize(be, BlockEntityPADetector.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityPADetector.SLOT_BATTERY,
                        8,
                        72,
                        IBatteryItem::isBattery));
        addSlot(new Slot(container(), BlockEntityPADetector.SLOT_CONTAINER_1, 62, 18));
        addSlot(new Slot(container(), BlockEntityPADetector.SLOT_CONTAINER_2, 80, 18));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container(),
                        BlockEntityPADetector.SLOT_OUTPUT_1,
                        62,
                        45));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container(),
                        BlockEntityPADetector.SLOT_OUTPUT_2,
                        80,
                        45));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityPADetector.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityPADetector.SLOT_BATTERY,
                                BlockEntityPADetector.SLOT_BATTERY + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityPADetector.SLOT_CONTAINER_1,
                            BlockEntityPADetector.SLOT_CONTAINER_2 + 1,
                            false);
                });
    }
}

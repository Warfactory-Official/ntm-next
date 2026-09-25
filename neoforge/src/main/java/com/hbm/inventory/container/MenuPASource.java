// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.albion.BlockEntityPASource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuPASource extends BlockEntityMenu<BlockEntityPASource> {

    public MenuPASource(int containerId, Inventory playerInv, BlockEntityPASource be) {
        super(ModMenus.PA_SOURCE.get(), containerId, be);
        checkContainerSize(be, BlockEntityPASource.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityPASource.SLOT_BATTERY,
                        8,
                        72,
                        IBatteryItem::isBattery));
        addSlot(new Slot(container(), BlockEntityPASource.SLOT_INPUT_1, 62, 16));
        addSlot(new Slot(container(), BlockEntityPASource.SLOT_INPUT_2, 80, 16));
        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityPASource.SLOT_CONTAINER_1,
                        62,
                        43,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityPASource.SLOT_CONTAINER_2,
                        80,
                        43,
                        SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityPASource.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityPASource.SLOT_BATTERY,
                                BlockEntityPASource.SLOT_BATTERY + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityPASource.SLOT_INPUT_1,
                            BlockEntityPASource.SLOT_INPUT_2 + 1,
                            false);
                });
    }
}

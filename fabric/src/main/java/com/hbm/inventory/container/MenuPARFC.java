// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.albion.BlockEntityPARFC;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuPARFC extends BlockEntityMenu<BlockEntityPARFC> {

    public MenuPARFC(int containerId, Inventory playerInv, BlockEntityPARFC be) {
        super(ModMenus.PA_RFC.get(), containerId, be);
        checkContainerSize(be, BlockEntityPARFC.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityPARFC.SLOT_BATTERY,
                        53,
                        72,
                        IBatteryItem::isBattery));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityPARFC.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(
                            stack,
                            BlockEntityPARFC.SLOT_BATTERY,
                            BlockEntityPARFC.SLOT_BATTERY + 1,
                            false);
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.storage.BlockEntityBatteryREDD;
import com.hbm.tileentity.machine.storage.BlockEntityMachineBattery;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuBatteryREDD extends BlockEntityMenu<BlockEntityBatteryREDD> {

    private final SyncedData data;

    public MenuBatteryREDD(int containerId, Inventory playerInv, BlockEntityBatteryREDD battery) {
        this(
                containerId,
                playerInv,
                battery,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityBatteryREDD.class)
                        : SyncedData.of(battery),
                battery);
    }

    private MenuBatteryREDD(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityBatteryREDD battery) {
        super(ModMenus.BATTERY_REDD.get(), containerId, battery, container);
        checkContainerSize(container, BlockEntityMachineBattery.SLOT_COUNT);
        this.data = data;

        addSlot(SlotFiltered.gated(container, BlockEntityMachineBattery.SLOT_CHARGE, 26, 53));
        addSlot(SlotFiltered.gated(container, BlockEntityMachineBattery.SLOT_DISCHARGE, 80, 53));
        addStandardInventorySlots(playerInv, 8, 99);
        addDataSlots(data);
    }

    public int getRedLow() {
        return data.getInt("redLow");
    }

    public int getRedHigh() {
        return data.getInt("redHigh");
    }

    public int getPriorityOrdinal() {
        return data.getInt("priority");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineBattery.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);

                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}

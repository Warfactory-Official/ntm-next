// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.storage.BlockEntityMachineBattery;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineBattery extends BlockEntityMenu<BlockEntityMachineBattery> {

    private final SyncedData data;

    public MenuMachineBattery(
            int containerId, Inventory playerInv, BlockEntityMachineBattery battery) {
        this(
                containerId,
                playerInv,
                battery,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineBattery.class)
                        : SyncedData.of(battery),
                battery);
    }

    private MenuMachineBattery(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineBattery battery) {
        super(ModMenus.MACHINE_BATTERY.get(), containerId, battery, container);
        checkContainerSize(container, BlockEntityMachineBattery.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineBattery.SLOT_CHARGE,
                        26,
                        17,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineBattery.SLOT_DISCHARGE,
                        26,
                        53,
                        IBatteryItem::isBattery));
        addStandardInventorySlots(playerInv, 8, 84);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public long getMaxPower() {
        return blockEntity().getMaxPower();
    }

    public long getPowerRemainingScaled(long i) {
        return blockEntity().getPowerRemainingScaled(i);
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

    public long getDelta() {
        return data.get("delta");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineBattery.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack, BlockEntityMachineBattery.SLOT_CHARGE, machineEnd, false);
                    return false;
                });
    }
}

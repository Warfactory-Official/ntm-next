// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.storage.BlockEntityBatterySocket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuBatterySocket extends BlockEntityMenu<BlockEntityBatterySocket> {

    private final SyncedData data;

    public MenuBatterySocket(
            int containerId, Inventory playerInv, BlockEntityBatterySocket socket) {
        this(
                containerId,
                playerInv,
                socket,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityBatterySocket.class)
                        : SyncedData.of(socket),
                socket);
    }

    private MenuBatterySocket(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityBatterySocket socket) {
        super(ModMenus.BATTERY_SOCKET.get(), containerId, socket, container);
        checkContainerSize(container, BlockEntityBatterySocket.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityBatterySocket.SLOT_BATTERY,
                        35,
                        35,
                        IBatteryItem::isBattery));
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

    public long getDelta() {
        return data.get("delta");
    }

    public ItemStack getBattery() {
        return container().getItem(BlockEntityBatterySocket.SLOT_BATTERY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityBatterySocket.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityBatterySocket.SLOT_BATTERY,
                                BlockEntityBatterySocket.SLOT_BATTERY + 1,
                                false);
                    return false;
                });
    }
}

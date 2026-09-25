// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotSmelting;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineElectricFurnace;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class MenuMachineElectricFurnace extends NtmContainerMenu {

    private final SyncedData data;

    public MenuMachineElectricFurnace(int containerId, Inventory playerInv) {
        this(
                containerId,
                playerInv,
                new SimpleContainer(BlockEntityMachineElectricFurnace.SLOT_COUNT),
                null,
                SyncedData.client(BlockEntityMachineElectricFurnace.class));
    }

    public MenuMachineElectricFurnace(
            int containerId, Inventory playerInv, BlockEntityMachineElectricFurnace be) {
        this(containerId, playerInv, be, be, SyncedData.of(be));
    }

    private MenuMachineElectricFurnace(
            int containerId,
            Inventory playerInv,
            Container container,
            @Nullable BlockEntityMachineElectricFurnace furnace,
            SyncedData data) {
        super(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), containerId, container);
        checkContainerSize(container, BlockEntityMachineElectricFurnace.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineElectricFurnace.SLOT_BATTERY,
                        152,
                        54,
                        IBatteryItem::isBattery));
        addSlot(new Slot(container, BlockEntityMachineElectricFurnace.SLOT_INPUT, 20, 35));
        addSlot(
                furnace != null
                        ? new SlotSmelting(
                                playerInv.player,
                                furnace,
                                BlockEntityMachineElectricFurnace.SLOT_OUTPUT,
                                80,
                                35)
                        : new SlotSmelting(
                                playerInv.player,
                                container,
                                BlockEntityMachineElectricFurnace.SLOT_OUTPUT,
                                80,
                                35));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineElectricFurnace.SLOT_UPGRADE, 111, 34));

        addStandardInventorySlots(playerInv, 8, 104);
        addDataSlots(data);
    }

    public int getProgress() {
        return data.getInt("progress");
    }

    public int getMaxProgress() {
        return data.getInt("maxProgress");
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineElectricFurnace.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineElectricFurnace.SLOT_BATTERY,
                                BlockEntityMachineElectricFurnace.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineElectricFurnace.SLOT_UPGRADE,
                                BlockEntityMachineElectricFurnace.SLOT_UPGRADE + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineElectricFurnace.SLOT_INPUT,
                            BlockEntityMachineElectricFurnace.SLOT_INPUT + 1,
                            false);
                });
    }
}

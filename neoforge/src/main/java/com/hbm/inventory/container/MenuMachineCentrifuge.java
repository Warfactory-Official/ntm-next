// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineCentrifuge;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineCentrifuge extends NtmContainerMenu {

    private final SyncedData data;

    public MenuMachineCentrifuge(int containerId, Inventory playerInv) {
        this(
                containerId,
                playerInv,
                new SimpleContainer(BlockEntityMachineCentrifuge.SLOT_COUNT),
                SyncedData.client(BlockEntityMachineCentrifuge.class));
    }

    public MenuMachineCentrifuge(
            int containerId, Inventory playerInv, BlockEntityMachineCentrifuge be) {
        this(containerId, playerInv, be, SyncedData.of(be));
    }

    private MenuMachineCentrifuge(
            int containerId, Inventory playerInv, Container container, SyncedData data) {
        super(ModMenus.MACHINE_CENTRIFUGE.get(), containerId, container);
        checkContainerSize(container, BlockEntityMachineCentrifuge.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, BlockEntityMachineCentrifuge.SLOT_INPUT, 44, 57));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineCentrifuge.SLOT_BATTERY,
                        8,
                        57,
                        IBatteryItem::isBattery));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 2, 70, 57));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 3, 90, 57));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 4, 110, 57));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 5, 130, 57));
        addSlot(new SlotUpgrade(container, 6, 156, 31));
        addSlot(new SlotUpgrade(container, 7, 156, 49));

        addStandardInventorySlots(playerInv, 11, 107);
        addDataSlots(data);
    }

    public int getProgress() {
        return data.getInt("progress");
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
                    int machineEnd = BlockEntityMachineCentrifuge.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCentrifuge.SLOT_BATTERY,
                                BlockEntityMachineCentrifuge.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCentrifuge.SLOT_UPGRADE_START,
                                BlockEntityMachineCentrifuge.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineCentrifuge.SLOT_INPUT,
                            BlockEntityMachineCentrifuge.SLOT_INPUT + 1,
                            false);
                });
    }
}

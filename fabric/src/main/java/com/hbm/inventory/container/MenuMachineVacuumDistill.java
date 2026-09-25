// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotDeprecated;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.oil.BlockEntityMachineVacuumDistill;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineVacuumDistill extends BlockEntityMenu<BlockEntityMachineVacuumDistill> {

    private final SyncedData data;

    public MenuMachineVacuumDistill(
            int containerId, Inventory playerInv, BlockEntityMachineVacuumDistill be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineVacuumDistill.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineVacuumDistill(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineVacuumDistill be) {
        super(ModMenus.MACHINE_VACUUM_DISTILL.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineVacuumDistill.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, BlockEntityMachineVacuumDistill.SLOT_BATTERY, 26, 90));
        addSlot(
                new SlotDeprecated(
                        container, BlockEntityMachineVacuumDistill.SLOT_CANISTER_DEAD_IN, 44, 90));
        addSlot(
                new SlotDeprecated(
                        container,
                        BlockEntityMachineVacuumDistill.SLOT_CANISTER_DEAD_OUT,
                        44,
                        108));
        addSlot(new Slot(container, 3, 80, 90));
        addSlot(new SlotFiltered(container, 4, 80, 108, SlotFiltered.NONE));
        addSlot(new Slot(container, 5, 98, 90));
        addSlot(new SlotFiltered(container, 6, 98, 108, SlotFiltered.NONE));
        addSlot(new Slot(container, 7, 116, 90));
        addSlot(new SlotFiltered(container, 8, 116, 108, SlotFiltered.NONE));
        addSlot(new Slot(container, 9, 134, 90));
        addSlot(new SlotFiltered(container, 10, 134, 108, SlotFiltered.NONE));
        addSlot(new Slot(container, BlockEntityMachineVacuumDistill.SLOT_FLUID_ID, 26, 108));

        addStandardInventorySlots(playerInv, 8, 156);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineVacuumDistill.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineVacuumDistill.SLOT_BATTERY,
                                BlockEntityMachineVacuumDistill.SLOT_BATTERY + 1,
                                false);
                    return moveItemStackTo(stack, 3, 4, false)
                            || moveItemStackTo(stack, 5, 6, false)
                            || moveItemStackTo(stack, 7, 8, false)
                            || moveItemStackTo(stack, 9, 10, false)
                            || moveItemStackTo(
                                    stack,
                                    BlockEntityMachineVacuumDistill.SLOT_FLUID_ID,
                                    BlockEntityMachineVacuumDistill.SLOT_FLUID_ID + 1,
                                    false);
                });
    }
}

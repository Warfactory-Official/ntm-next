// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.oil.BlockEntityMachineRefinery;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineRefinery extends BlockEntityMenu<BlockEntityMachineRefinery> {

    private final SyncedData data;

    public MenuMachineRefinery(
            int containerId, Inventory playerInv, BlockEntityMachineRefinery be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineRefinery.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineRefinery(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineRefinery be) {
        super(ModMenus.MACHINE_REFINERY.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineRefinery.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineRefinery.SLOT_BATTERY,
                        158,
                        108,
                        IBatteryItem::isBattery));
        addSlot(new Slot(container, 1, 12, 90));
        addSlot(new SlotFiltered(container, 2, 12, 108, SlotFiltered.NONE));
        addSlot(new Slot(container, 3, 64, 90));
        addSlot(new SlotFiltered(container, 4, 64, 108, SlotFiltered.NONE));
        addSlot(new Slot(container, 5, 82, 90));
        addSlot(new SlotFiltered(container, 6, 82, 108, SlotFiltered.NONE));
        addSlot(new Slot(container, 7, 100, 90));
        addSlot(new SlotFiltered(container, 8, 100, 108, SlotFiltered.NONE));
        addSlot(new Slot(container, 9, 118, 90));
        addSlot(new SlotFiltered(container, 10, 118, 108, SlotFiltered.NONE));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineRefinery.SLOT_SOLID_OUT,
                        38,
                        90));
        addSlot(new Slot(container, BlockEntityMachineRefinery.SLOT_FLUID_ID, 38, 108));

        addStandardInventorySlots(playerInv, 11, 158);
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
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineRefinery.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineRefinery.SLOT_BATTERY,
                                BlockEntityMachineRefinery.SLOT_BATTERY + 1,
                                false);
                    return moveItemStackTo(stack, 1, 2, false)
                            || moveItemStackTo(stack, 3, 4, false)
                            || moveItemStackTo(stack, 5, 6, false)
                            || moveItemStackTo(stack, 7, 8, false)
                            || moveItemStackTo(stack, 9, 10, false)
                            || moveItemStackTo(
                                    stack,
                                    BlockEntityMachineRefinery.SLOT_FLUID_ID,
                                    BlockEntityMachineRefinery.SLOT_FLUID_ID + 1,
                                    false);
                });
    }
}

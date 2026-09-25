// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityMachineHydrotreater;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineHydrotreater extends BlockEntityMenu<BlockEntityMachineHydrotreater> {

    private final SyncedData data;

    public MenuMachineHydrotreater(
            int containerId, Inventory playerInv, BlockEntityMachineHydrotreater be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityMachineHydrotreater.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuMachineHydrotreater(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityMachineHydrotreater be) {
        super(ModMenus.MACHINE_HYDROTREATER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineHydrotreater.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineHydrotreater.SLOT_BATTERY,
                        17,
                        90,
                        IBatteryItem::isBattery));
        addSlot(new Slot(container, BlockEntityMachineHydrotreater.SLOT_INPUT_IN, 35, 90));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineHydrotreater.SLOT_INPUT_OUT,
                        35,
                        108,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, BlockEntityMachineHydrotreater.SLOT_OUTPUT_IN_START, 125, 90));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineHydrotreater.SLOT_OUTPUT_OUT_START,
                        125,
                        108,
                        SlotFiltered.NONE));
        addSlot(
                new Slot(
                        container,
                        BlockEntityMachineHydrotreater.SLOT_OUTPUT_IN_START + 2,
                        143,
                        90));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineHydrotreater.SLOT_OUTPUT_OUT_START + 2,
                        143,
                        108,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineHydrotreater.SLOT_FLUID_ID,
                        17,
                        108,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineHydrotreater.SLOT_CATALYST,
                        89,
                        36,
                        stack -> stack.is(ModItems.CATALYTIC_CONVERTER.get())));

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
                    int machineEnd = BlockEntityMachineHydrotreater.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineHydrotreater.SLOT_BATTERY,
                                BlockEntityMachineHydrotreater.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineHydrotreater.SLOT_FLUID_ID,
                                BlockEntityMachineHydrotreater.SLOT_FLUID_ID + 1,
                                false);
                    if (stack.is(ModItems.CATALYTIC_CONVERTER.get()))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineHydrotreater.SLOT_CATALYST,
                                BlockEntityMachineHydrotreater.SLOT_CATALYST + 1,
                                false);
                    return moveItemStackTo(
                                    stack,
                                    BlockEntityMachineHydrotreater.SLOT_INPUT_IN,
                                    BlockEntityMachineHydrotreater.SLOT_INPUT_IN + 1,
                                    false)
                            || moveItemStackTo(
                                    stack,
                                    BlockEntityMachineHydrotreater.SLOT_OUTPUT_IN_START,
                                    BlockEntityMachineHydrotreater.SLOT_OUTPUT_IN_START + 1,
                                    false)
                            || moveItemStackTo(
                                    stack,
                                    BlockEntityMachineHydrotreater.SLOT_OUTPUT_IN_START + 2,
                                    BlockEntityMachineHydrotreater.SLOT_OUTPUT_IN_START + 3,
                                    false);
                });
    }
}

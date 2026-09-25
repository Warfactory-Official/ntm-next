// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.handler.FuelHandler;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.tileentity.machine.BlockEntityMachineWoodBurner;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MenuMachineWoodBurner extends BlockEntityMenu<BlockEntityMachineWoodBurner> {

    public MenuMachineWoodBurner(
            int containerId, Inventory playerInv, BlockEntityMachineWoodBurner be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineWoodBurner(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineWoodBurner be) {
        super(ModMenus.MACHINE_WOOD_BURNER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineWoodBurner.SLOT_COUNT);

        addSlot(new Slot(container, BlockEntityMachineWoodBurner.SLOT_FUEL, 26, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineWoodBurner.SLOT_ASH,
                        26,
                        54,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineWoodBurner.SLOT_FLUID_ID,
                        98,
                        54,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));
        addSlot(new Slot(container, BlockEntityMachineWoodBurner.SLOT_FLUID_IN, 98, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineWoodBurner.SLOT_FLUID_OUT,
                        98,
                        36,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineWoodBurner.SLOT_BATTERY,
                        143,
                        54,
                        IBatteryItem::isBattery));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineWoodBurner.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineWoodBurner.SLOT_BATTERY,
                                BlockEntityMachineWoodBurner.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineWoodBurner.SLOT_FLUID_ID,
                                BlockEntityMachineWoodBurner.SLOT_FLUID_ID + 1,
                                false);
                    if (FuelHandler.getBurnTime(player.level(), stack) > 0)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineWoodBurner.SLOT_FUEL,
                                BlockEntityMachineWoodBurner.SLOT_FUEL + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineWoodBurner.SLOT_FLUID_IN,
                            BlockEntityMachineWoodBurner.SLOT_FLUID_IN + 1,
                            false);
                });
    }
}

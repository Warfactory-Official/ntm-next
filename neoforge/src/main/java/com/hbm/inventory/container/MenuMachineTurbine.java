// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.tileentity.machine.BlockEntityMachineTurbine;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MenuMachineTurbine extends BlockEntityMenu<BlockEntityMachineTurbine> {

    public MenuMachineTurbine(int containerId, Inventory playerInv, BlockEntityMachineTurbine be) {
        super(ModMenus.MACHINE_TURBINE.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineTurbine.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbine.SLOT_FLUID_ID_IN,
                        8,
                        17,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));
        addSlot(
                new SlotFiltered(
                        be, BlockEntityMachineTurbine.SLOT_FLUID_ID_OUT, 8, 53, SlotFiltered.NONE));
        addSlot(new Slot(be, BlockEntityMachineTurbine.SLOT_CONTAINER_IN, 44, 17));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbine.SLOT_CONTAINER_OUT,
                        44,
                        53,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbine.SLOT_BATTERY,
                        98,
                        53,
                        IBatteryItem::isBattery));
        addSlot(new Slot(be, BlockEntityMachineTurbine.SLOT_EMPTY_CONTAINER_IN, 152, 17));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbine.SLOT_FILLED_CONTAINER_OUT,
                        152,
                        53,
                        SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineTurbine.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineTurbine.SLOT_BATTERY,
                                BlockEntityMachineTurbine.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineTurbine.SLOT_FLUID_ID_IN,
                                BlockEntityMachineTurbine.SLOT_FLUID_ID_IN + 1,
                                false);

                    if (isFluidContainer(stack))
                        return moveItemStackTo(
                                        stack,
                                        BlockEntityMachineTurbine.SLOT_CONTAINER_IN,
                                        BlockEntityMachineTurbine.SLOT_CONTAINER_IN + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        BlockEntityMachineTurbine.SLOT_EMPTY_CONTAINER_IN,
                                        BlockEntityMachineTurbine.SLOT_EMPTY_CONTAINER_IN + 1,
                                        false);
                    return false;
                });
    }
}

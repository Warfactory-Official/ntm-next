// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityMachineLargeTurbine;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineLargeTurbine extends BlockEntityMenu<BlockEntityMachineLargeTurbine> {

    public MenuMachineLargeTurbine(
            int containerId, Inventory playerInv, BlockEntityMachineLargeTurbine be) {
        super(ModMenus.MACHINE_LARGE_TURBINE.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineLargeTurbine.SLOT_COUNT);

        addSlot(new Slot(be, BlockEntityMachineLargeTurbine.SLOT_FLUID_ID, 8, 17));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineLargeTurbine.SLOT_FLUID_ID_OUT,
                        8,
                        53,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineLargeTurbine.SLOT_CONTAINER_IN,
                        44,
                        17,
                        FluidTankNTM::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineLargeTurbine.SLOT_CONTAINER_OUT,
                        44,
                        53,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineLargeTurbine.SLOT_BATTERY,
                        98,
                        53,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineLargeTurbine.SLOT_UNLOAD_IN,
                        152,
                        17,
                        FluidTankNTM::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineLargeTurbine.SLOT_UNLOAD_OUT,
                        152,
                        53,
                        SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineLargeTurbine.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineLargeTurbine.SLOT_BATTERY,
                                BlockEntityMachineLargeTurbine.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineLargeTurbine.SLOT_FLUID_ID,
                                BlockEntityMachineLargeTurbine.SLOT_FLUID_ID + 1,
                                false);
                    if (FluidTankNTM.isFluidContainer(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineLargeTurbine.SLOT_CONTAINER_IN,
                                BlockEntityMachineLargeTurbine.SLOT_CONTAINER_IN + 1,
                                false);
                    return false;
                });
    }
}

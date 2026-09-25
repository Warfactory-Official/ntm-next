// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.tileentity.machine.BlockEntityMachineDiesel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MenuMachineDiesel extends BlockEntityMenu<BlockEntityMachineDiesel> {

    public MenuMachineDiesel(int containerId, Inventory playerInv, BlockEntityMachineDiesel be) {
        super(ModMenus.MACHINE_DIESEL.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineDiesel.SLOT_COUNT);

        addSlot(new Slot(be, BlockEntityMachineDiesel.SLOT_FLUID_IN, 17, 17));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineDiesel.SLOT_CONTAINER_OUT,
                        17,
                        53,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineDiesel.SLOT_BATTERY,
                        141,
                        71,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineDiesel.SLOT_FLUID_ID,
                        35,
                        71,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 121);
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
                    int machineEnd = BlockEntityMachineDiesel.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineDiesel.SLOT_BATTERY,
                                BlockEntityMachineDiesel.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineDiesel.SLOT_FLUID_ID,
                                BlockEntityMachineDiesel.SLOT_FLUID_ID + 1,
                                false);
                    if (isFluidContainer(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineDiesel.SLOT_FLUID_IN,
                                BlockEntityMachineDiesel.SLOT_FLUID_IN + 1,
                                false);
                    return false;
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.oil.BlockEntityMachineGasFlare;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MenuMachineGasFlare extends BlockEntityMenu<BlockEntityMachineGasFlare> {

    public MenuMachineGasFlare(
            int containerId, Inventory playerInv, BlockEntityMachineGasFlare be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineGasFlare(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineGasFlare be) {
        super(ModMenus.MACHINE_GAS_FLARE.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineGasFlare.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineGasFlare.SLOT_BATTERY,
                        143,
                        71,
                        IBatteryItem::isBattery));
        addSlot(new Slot(container, BlockEntityMachineGasFlare.SLOT_FLUID_IN, 17, 17));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineGasFlare.SLOT_FLUID_OUT,
                        17,
                        53,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, BlockEntityMachineGasFlare.SLOT_FLUID_ID, 35, 71));
        addSlot(new SlotUpgrade(container, BlockEntityMachineGasFlare.SLOT_UPGRADE_START, 80, 71));
        addSlot(new SlotUpgrade(container, BlockEntityMachineGasFlare.SLOT_UPGRADE_END, 98, 71));

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
                    int machineEnd = BlockEntityMachineGasFlare.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineGasFlare.SLOT_FLUID_ID,
                                BlockEntityMachineGasFlare.SLOT_FLUID_ID + 1,
                                false);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineGasFlare.SLOT_BATTERY,
                                BlockEntityMachineGasFlare.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineGasFlare.SLOT_UPGRADE_START,
                                BlockEntityMachineGasFlare.SLOT_UPGRADE_END + 1,
                                false);
                    if (isFluidContainer(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineGasFlare.SLOT_FLUID_IN,
                                BlockEntityMachineGasFlare.SLOT_FLUID_IN + 1,
                                false);
                    return false;
                });
    }
}

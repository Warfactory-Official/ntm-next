// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.oil.BlockEntityOilDrillBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineOilWell extends BlockEntityMenu<BlockEntityOilDrillBase> {

    private static final int VISIBLE_MACHINE_SLOTS = 7;

    public MenuMachineOilWell(int containerId, Inventory playerInv, BlockEntityOilDrillBase be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineOilWell(
            int containerId, Inventory playerInv, Container container, BlockEntityOilDrillBase be) {
        super(ModMenus.MACHINE_WELL.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityOilDrillBase.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityOilDrillBase.SLOT_BATTERY,
                        8,
                        58,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityOilDrillBase.SLOT_OIL_IN,
                        94,
                        22,
                        MenuMachineOilWell::isContainer));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityOilDrillBase.SLOT_OIL_OUT,
                        94,
                        58,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityOilDrillBase.SLOT_GAS_IN,
                        130,
                        22,
                        MenuMachineOilWell::isContainer));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityOilDrillBase.SLOT_GAS_OUT,
                        130,
                        58,
                        SlotFiltered.NONE));
        addSlot(new SlotUpgrade(container, BlockEntityOilDrillBase.SLOT_UPGRADE_START, 156, 36));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityOilDrillBase.SLOT_UPGRADE_START + 1, 156, 54));

        addStandardInventorySlots(playerInv, 12, 108);
    }

    private static boolean isContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = VISIBLE_MACHINE_SLOTS;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityOilDrillBase.SLOT_BATTERY,
                                BlockEntityOilDrillBase.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityOilDrillBase.SLOT_UPGRADE_START,
                                VISIBLE_MACHINE_SLOTS,
                                false);
                    if (isContainer(stack))
                        return moveItemStackTo(
                                        stack,
                                        BlockEntityOilDrillBase.SLOT_OIL_IN,
                                        BlockEntityOilDrillBase.SLOT_OIL_IN + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        BlockEntityOilDrillBase.SLOT_GAS_IN,
                                        BlockEntityOilDrillBase.SLOT_GAS_IN + 1,
                                        false);
                    return false;
                });
    }
}

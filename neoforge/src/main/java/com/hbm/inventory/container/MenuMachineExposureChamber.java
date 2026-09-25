// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineExposureChamber;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineExposureChamber extends BlockEntityMenu<BlockEntityMachineExposureChamber> {

    private static final int PARTICLE = 0;
    private static final int OUTPUT = 3;
    private static final int BATTERY = 4;
    private static final int UPGRADE_START = 5;
    private static final int UPGRADE_END = 6;
    private static final int MACHINE_SLOTS = 7;

    public MenuMachineExposureChamber(
            int containerId, Inventory playerInv, BlockEntityMachineExposureChamber be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineExposureChamber(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineExposureChamber be) {
        super(ModMenus.MACHINE_EXPOSURE_CHAMBER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineExposureChamber.SLOT_COUNT);

        addSlot(new Slot(container, BlockEntityMachineExposureChamber.SLOT_PARTICLE, 8, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineExposureChamber.SLOT_CONTAINER,
                        8,
                        54,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, BlockEntityMachineExposureChamber.SLOT_INGREDIENT, 80, 36));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlockEntityMachineExposureChamber.SLOT_OUTPUT,
                        116,
                        36));
        addSlot(new Slot(container, BlockEntityMachineExposureChamber.SLOT_BATTERY, 152, 54));
        addSlot(new Slot(container, BlockEntityMachineExposureChamber.SLOT_UPGRADE_START, 44, 54));
        addSlot(new Slot(container, BlockEntityMachineExposureChamber.SLOT_UPGRADE_END, 62, 54));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < MACHINE_SLOTS)
                        return moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true);
                    if (ItemMachineUpgrade.isUpgrade(stack)) {
                        return moveItemStackTo(stack, UPGRADE_START, UPGRADE_END + 1, false);
                    }
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(stack, BATTERY, BATTERY + 1, false);
                    return moveItemStackTo(stack, PARTICLE, OUTPUT, false);
                });
    }
}

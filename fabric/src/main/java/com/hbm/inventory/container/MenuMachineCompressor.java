// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineCompressor;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineCompressor extends BlockEntityMenu<BlockEntityMachineCompressor> {

    public MenuMachineCompressor(
            int containerId, Inventory playerInv, BlockEntityMachineCompressor be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineCompressor(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineCompressor be) {
        super(ModMenus.MACHINE_COMPRESSOR.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineCompressor.SLOT_COUNT);

        addSlot(new Slot(container, BlockEntityMachineCompressor.SLOT_FLUID_ID, 17, 72));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineCompressor.SLOT_BATTERY,
                        152,
                        72,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineCompressor.SLOT_UPGRADE_START, 52, 72));
        addSlot(new SlotUpgrade(container, BlockEntityMachineCompressor.SLOT_UPGRADE_END, 70, 72));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineCompressor.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCompressor.SLOT_FLUID_ID,
                                BlockEntityMachineCompressor.SLOT_FLUID_ID + 1,
                                false);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCompressor.SLOT_BATTERY,
                                BlockEntityMachineCompressor.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCompressor.SLOT_UPGRADE_START,
                                BlockEntityMachineCompressor.SLOT_UPGRADE_END + 1,
                                false);
                    return false;
                });
    }
}

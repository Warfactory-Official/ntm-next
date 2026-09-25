// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.ItemFELCrystal;
import com.hbm.tileentity.machine.BlockEntityFEL;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuFEL extends BlockEntityMenu<BlockEntityFEL> {

    public MenuFEL(int containerId, Inventory playerInv, BlockEntityFEL be) {
        this(containerId, playerInv, be, be);
    }

    private MenuFEL(int containerId, Inventory playerInv, Container container, BlockEntityFEL be) {
        super(ModMenus.MACHINE_FEL.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityFEL.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container, BlockEntityFEL.SLOT_BATTERY, 182, 144, IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityFEL.SLOT_CRYSTAL,
                        141,
                        23,
                        stack -> stack.getItem() instanceof ItemFELCrystal));

        addStandardInventorySlots(playerInv, 8, 83);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityFEL.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityFEL.SLOT_BATTERY,
                                BlockEntityFEL.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof ItemFELCrystal)
                        return moveItemStackTo(
                                stack,
                                BlockEntityFEL.SLOT_CRYSTAL,
                                BlockEntityFEL.SLOT_CRYSTAL + 1,
                                false);
                    return false;
                });
    }
}

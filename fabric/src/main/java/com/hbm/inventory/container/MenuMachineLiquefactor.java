// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.oil.BlockEntityMachineLiquefactor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineLiquefactor extends BlockEntityMenu<BlockEntityMachineLiquefactor> {

    public MenuMachineLiquefactor(
            int containerId, Inventory playerInv, BlockEntityMachineLiquefactor be) {
        super(ModMenus.MACHINE_LIQUEFACTOR.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineLiquefactor.SLOT_COUNT);

        addSlot(new Slot(be, BlockEntityMachineLiquefactor.SLOT_INPUT, 35, 54));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineLiquefactor.SLOT_BATTERY,
                        134,
                        72,
                        IBatteryItem::isBattery));
        addSlot(new SlotUpgrade(be, BlockEntityMachineLiquefactor.SLOT_UPGRADE_START, 98, 36));
        addSlot(new SlotUpgrade(be, BlockEntityMachineLiquefactor.SLOT_UPGRADE_END, 98, 54));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineLiquefactor.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineLiquefactor.SLOT_BATTERY,
                                BlockEntityMachineLiquefactor.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineLiquefactor.SLOT_UPGRADE_START,
                                BlockEntityMachineLiquefactor.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineLiquefactor.SLOT_INPUT,
                            BlockEntityMachineLiquefactor.SLOT_INPUT + 1,
                            false);
                });
    }
}

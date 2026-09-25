// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.oil.BlockEntityMachineSolidifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineSolidifier extends BlockEntityMenu<BlockEntityMachineSolidifier> {

    public MenuMachineSolidifier(
            int containerId, Inventory playerInv, BlockEntityMachineSolidifier be) {
        super(ModMenus.MACHINE_SOLIDIFIER.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineSolidifier.SLOT_COUNT);

        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityMachineSolidifier.SLOT_OUTPUT, 71, 45));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineSolidifier.SLOT_BATTERY,
                        134,
                        72,
                        IBatteryItem::isBattery));
        addSlot(new SlotUpgrade(be, BlockEntityMachineSolidifier.SLOT_UPGRADE_START, 98, 36));
        addSlot(new SlotUpgrade(be, BlockEntityMachineSolidifier.SLOT_UPGRADE_END, 98, 54));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineSolidifier.SLOT_FLUID_ID,
                        71,
                        72,
                        s -> s.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineSolidifier.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSolidifier.SLOT_BATTERY,
                                BlockEntityMachineSolidifier.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSolidifier.SLOT_UPGRADE_START,
                                BlockEntityMachineSolidifier.SLOT_UPGRADE_END + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineSolidifier.SLOT_FLUID_ID,
                                BlockEntityMachineSolidifier.SLOT_FLUID_ID + 1,
                                false);
                    return false;
                });
    }
}

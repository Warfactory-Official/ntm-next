// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineElectrolyser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineElectrolyserFluid extends BlockEntityMenu<BlockEntityMachineElectrolyser> {

    private static final int MACHINE_SLOTS = 14;

    public MenuMachineElectrolyserFluid(
            int containerId, Inventory playerInv, BlockEntityMachineElectrolyser be) {
        super(ModMenus.MACHINE_ELECTROLYSER_FLUID.get(), containerId, be);

        addSlot(new Slot(be, 0, 186, 109));
        addSlot(new Slot(be, 1, 186, 140));
        addSlot(new Slot(be, 2, 186, 158));
        addSlot(new Slot(be, 3, 6, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 4, 6, 54));
        addSlot(new Slot(be, 5, 24, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 6, 24, 54));
        addSlot(new Slot(be, 7, 78, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 8, 78, 54));
        addSlot(new Slot(be, 9, 134, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 10, 134, 54));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 11, 154, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 12, 154, 36));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 13, 154, 54));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int invEnd = slots.size();

                    if (index < MACHINE_SLOTS)
                        return moveItemStackTo(stack, MACHINE_SLOTS, invEnd, true);
                    if (IBatteryItem.isBattery(stack)) return moveItemStackTo(stack, 0, 1, false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(stack, 1, 3, false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(stack, 3, 4, false);
                    return false;
                });
    }
}

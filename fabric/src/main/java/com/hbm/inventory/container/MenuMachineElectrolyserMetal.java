// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineElectrolyser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineElectrolyserMetal extends BlockEntityMenu<BlockEntityMachineElectrolyser> {

    private static final int MACHINE_SLOTS = 10;
    private static final int CRYSTAL_CONTAINER_SLOT = 3;

    public MenuMachineElectrolyserMetal(
            int containerId, Inventory playerInv, BlockEntityMachineElectrolyser be) {
        super(ModMenus.MACHINE_ELECTROLYSER_METAL.get(), containerId, be);

        addSlot(new Slot(be, 0, 186, 109));
        addSlot(new Slot(be, 1, 186, 140));
        addSlot(new Slot(be, 2, 186, 158));
        addSlot(new Slot(be, 14, 10, 22));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 15, 136, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 16, 154, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 17, 136, 36));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 18, 154, 36));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 19, 136, 54));
        addSlot(new SlotRecipeOutput(playerInv.player, be, 20, 154, 54));

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

                    return moveItemStackTo(
                            stack, CRYSTAL_CONTAINER_SLOT, CRYSTAL_CONTAINER_SLOT + 1, false);
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.oil.BlockEntityMachineCoker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineCoker extends BlockEntityMenu<BlockEntityMachineCoker> {

    public MenuMachineCoker(int containerId, Inventory playerInv, BlockEntityMachineCoker be) {
        super(ModMenus.MACHINE_COKER.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineCoker.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineCoker.SLOT_FLUID_ID,
                        35,
                        72,
                        s -> s.getItem() instanceof FluidIdentifierItem));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityMachineCoker.SLOT_OUTPUT, 97, 27));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineCoker.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineCoker.SLOT_FLUID_ID,
                                BlockEntityMachineCoker.SLOT_FLUID_ID + 1,
                                false);
                    return false;
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotSmelting;
import com.hbm.tileentity.machine.BlockEntityFurnaceCombination;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuFurnaceCombination extends BlockEntityMenu<BlockEntityFurnaceCombination> {

    public MenuFurnaceCombination(
            int containerId, Inventory playerInv, BlockEntityFurnaceCombination be) {
        this(containerId, playerInv, be, be);
    }

    private MenuFurnaceCombination(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityFurnaceCombination be) {
        super(ModMenus.FURNACE_COMBINATION.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityFurnaceCombination.SLOT_COUNT);

        addSlot(new Slot(container, BlockEntityFurnaceCombination.SLOT_INPUT, 26, 36));
        addSlot(
                new SlotSmelting(
                        playerInv.player,
                        container,
                        BlockEntityFurnaceCombination.SLOT_OUTPUT,
                        89,
                        36));

        addSlot(new Slot(container, 2, 136, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 3, 136, 54));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityFurnaceCombination.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    return moveItemStackTo(
                            stack,
                            BlockEntityFurnaceCombination.SLOT_INPUT,
                            BlockEntityFurnaceCombination.SLOT_INPUT + 1,
                            false);
                });
    }
}

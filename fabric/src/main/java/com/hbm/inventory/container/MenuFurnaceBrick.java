// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotSmelting;
import com.hbm.tileentity.machine.BlockEntityFurnaceBrick;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuFurnaceBrick extends BlockEntityMenu<BlockEntityFurnaceBrick> {

    public MenuFurnaceBrick(int containerId, Inventory playerInv, BlockEntityFurnaceBrick be) {
        this(containerId, playerInv, be, be);
    }

    private MenuFurnaceBrick(
            int containerId, Inventory playerInv, Container container, BlockEntityFurnaceBrick be) {
        super(ModMenus.FURNACE_BRICK.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityFurnaceBrick.SLOT_COUNT);

        addSlot(new Slot(container, BlockEntityFurnaceBrick.SLOT_INPUT, 62, 35));
        addSlot(new Slot(container, BlockEntityFurnaceBrick.SLOT_FUEL, 35, 17));
        addSlot(
                new SlotSmelting(
                        playerInv.player, be, BlockEntityFurnaceBrick.SLOT_OUTPUT, 116, 35));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, container, BlockEntityFurnaceBrick.SLOT_ASH, 35, 53));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityFurnaceBrick.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);

                    if (container().canPlaceItem(BlockEntityFurnaceBrick.SLOT_FUEL, stack))
                        return moveItemStackTo(
                                        stack,
                                        BlockEntityFurnaceBrick.SLOT_FUEL,
                                        BlockEntityFurnaceBrick.SLOT_FUEL + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        BlockEntityFurnaceBrick.SLOT_INPUT,
                                        BlockEntityFurnaceBrick.SLOT_INPUT + 1,
                                        false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityFurnaceBrick.SLOT_INPUT,
                            BlockEntityFurnaceBrick.SLOT_INPUT + 1,
                            false);
                });
    }
}

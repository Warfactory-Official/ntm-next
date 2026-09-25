// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.handler.FuelHandler;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.ItemStamp;
import com.hbm.tileentity.machine.BlockEntityMachinePress;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachinePress extends BlockEntityMenu<BlockEntityMachinePress> {

    public MenuMachinePress(int containerId, Inventory playerInv, BlockEntityMachinePress be) {
        super(ModMenus.MACHINE_PRESS.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachinePress.SLOT_COUNT);

        addSlot(new Slot(be, BlockEntityMachinePress.SLOT_FUEL, 26, 53));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachinePress.SLOT_STAMP,
                        80,
                        17,
                        stack -> stack.getItem() instanceof ItemStamp));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachinePress.SLOT_INPUT,
                        80,
                        53,
                        stack -> !(stack.getItem() instanceof ItemStamp)));

        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityMachinePress.SLOT_OUTPUT, 140, 35));

        for (int i = 0; i < BlockEntityMachinePress.SLOT_STORAGE_COUNT; i++) {
            addSlot(new Slot(be, BlockEntityMachinePress.SLOT_STORAGE_START + i, 8 + i * 18, 84));
        }

        addStandardInventorySlots(playerInv, 8, 132);
    }

    public int getSpeedPercent() {
        return blockEntity().speed * 100 / BlockEntityMachinePress.MAX_SPEED;
    }

    public int getOperationsLeft() {
        return blockEntity().burnTime / BlockEntityMachinePress.FUEL_PER_OP;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachinePress.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (FuelHandler.getBurnTime(player.level(), stack) > 0)
                        return moveItemStackTo(
                                        stack,
                                        BlockEntityMachinePress.SLOT_FUEL,
                                        BlockEntityMachinePress.SLOT_FUEL + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        BlockEntityMachinePress.SLOT_STORAGE_START,
                                        machineEnd,
                                        false);
                    if (stack.getItem() instanceof ItemStamp)
                        return moveItemStackTo(
                                        stack,
                                        BlockEntityMachinePress.SLOT_STAMP,
                                        BlockEntityMachinePress.SLOT_STAMP + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        BlockEntityMachinePress.SLOT_STORAGE_START,
                                        machineEnd,
                                        false);
                    return moveItemStackTo(
                                    stack,
                                    BlockEntityMachinePress.SLOT_INPUT,
                                    BlockEntityMachinePress.SLOT_INPUT + 1,
                                    false)
                            || moveItemStackTo(
                                    stack,
                                    BlockEntityMachinePress.SLOT_STORAGE_START,
                                    machineEnd,
                                    false);
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.oil.BlockEntityMachinePyroOven;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachinePyroOven extends BlockEntityMenu<BlockEntityMachinePyroOven> {

    public MenuMachinePyroOven(
            int containerId, Inventory playerInv, BlockEntityMachinePyroOven be) {
        super(ModMenus.MACHINE_PYROOVEN.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachinePyroOven.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachinePyroOven.SLOT_BATTERY,
                        152,
                        72,
                        IBatteryItem::isBattery));
        addSlot(new Slot(be, BlockEntityMachinePyroOven.SLOT_ITEM_INPUT, 35, 45));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityMachinePyroOven.SLOT_ITEM_OUTPUT, 89, 45));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachinePyroOven.SLOT_FLUID_ID,
                        8,
                        72,
                        s -> s.getItem() instanceof FluidIdentifierItem));
        addSlot(new SlotUpgrade(be, BlockEntityMachinePyroOven.SLOT_UPGRADE_START, 71, 72));
        addSlot(new SlotUpgrade(be, BlockEntityMachinePyroOven.SLOT_UPGRADE_END, 89, 72));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachinePyroOven.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachinePyroOven.SLOT_BATTERY,
                                BlockEntityMachinePyroOven.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachinePyroOven.SLOT_FLUID_ID,
                                BlockEntityMachinePyroOven.SLOT_FLUID_ID + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachinePyroOven.SLOT_UPGRADE_START,
                                BlockEntityMachinePyroOven.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachinePyroOven.SLOT_ITEM_INPUT,
                            BlockEntityMachinePyroOven.SLOT_ITEM_INPUT + 1,
                            false);
                });
    }
}

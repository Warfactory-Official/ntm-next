// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.tileentity.machine.BlockEntityMachineOreSlopper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuMachineOreSlopper extends BlockEntityMenu<BlockEntityMachineOreSlopper> {

    public MenuMachineOreSlopper(
            int containerId, Inventory playerInv, BlockEntityMachineOreSlopper be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineOreSlopper(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineOreSlopper be) {
        super(ModMenus.MACHINE_ORE_SLOPPER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityMachineOreSlopper.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineOreSlopper.SLOT_BATTERY,
                        8,
                        72,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineOreSlopper.SLOT_FLUID_ID,
                        26,
                        72,
                        s -> s.getItem() instanceof FluidIdentifierItem));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineOreSlopper.SLOT_INPUT,
                        71,
                        27,
                        s -> s.getItem() == ModItems.BEDROCK_ORE_BASE.get()));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 3, 134, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 4, 152, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 5, 134, 36));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 6, 152, 36));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 7, 134, 54));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 8, 152, 54));
        addSlot(
                new SlotUpgrade(
                        container, BlockEntityMachineOreSlopper.SLOT_UPGRADE_START, 62, 72));
        addSlot(new SlotUpgrade(container, BlockEntityMachineOreSlopper.SLOT_UPGRADE_END, 80, 72));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityMachineOreSlopper.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineOreSlopper.SLOT_BATTERY,
                                BlockEntityMachineOreSlopper.SLOT_BATTERY + 1,
                                false);
                    if (ItemMachineUpgrade.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineOreSlopper.SLOT_UPGRADE_START,
                                BlockEntityMachineOreSlopper.SLOT_UPGRADE_END + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineOreSlopper.SLOT_FLUID_ID,
                                BlockEntityMachineOreSlopper.SLOT_FLUID_ID + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityMachineOreSlopper.SLOT_INPUT,
                            BlockEntityMachineOreSlopper.SLOT_INPUT + 1,
                            false);
                });
    }
}

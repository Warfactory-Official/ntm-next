// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityMachineTurbineGas;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class MenuMachineTurbineGas extends BlockEntityMenu<BlockEntityMachineTurbineGas> {

    public MenuMachineTurbineGas(
            int containerId, Inventory playerInv, BlockEntityMachineTurbineGas be) {
        super(ModMenus.MACHINE_TURBINEGAS.get(), containerId, be);
        checkContainerSize(be, BlockEntityMachineTurbineGas.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbineGas.SLOT_BATTERY,
                        8,
                        109,
                        IBatteryItem::isBattery));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityMachineTurbineGas.SLOT_FLUID_ID,
                        36,
                        17,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 141);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityMachineTurbineGas.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (IBatteryItem.isBattery(stack))
                        return moveItemStackTo(
                                stack,
                                BlockEntityMachineTurbineGas.SLOT_BATTERY,
                                BlockEntityMachineTurbineGas.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof FluidIdentifierItem) {
                        Fluid type =
                                stack.getOrDefault(
                                                ModDataComponents.FLUID_IDENTIFIER.get(),
                                                FluidIdentifierData.EMPTY)
                                        .primary();
                        return BlockEntityMachineTurbineGas.isGasFuel(type)
                                && moveItemStackTo(
                                        stack,
                                        BlockEntityMachineTurbineGas.SLOT_FLUID_ID,
                                        BlockEntityMachineTurbineGas.SLOT_FLUID_ID + 1,
                                        false);
                    }
                    return false;
                });
    }
}

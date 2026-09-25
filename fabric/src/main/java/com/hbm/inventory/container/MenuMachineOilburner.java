// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityHeaterOilburner;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuMachineOilburner extends BlockEntityMenu<BlockEntityHeaterOilburner> {

    public MenuMachineOilburner(
            int containerId, Inventory playerInv, BlockEntityHeaterOilburner be) {
        this(containerId, playerInv, be, be);
    }

    private MenuMachineOilburner(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityHeaterOilburner be) {
        super(ModMenus.HEATER_OILBURNER.get(), containerId, be, container);
        checkContainerSize(container, 3);
        container.startOpen(playerInv.player);

        addSlot(new Slot(container, BlockEntityHeaterOilburner.SLOT_FLUID_IN, 26, 17));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityHeaterOilburner.SLOT_FLUID_OUT,
                        26,
                        53,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityHeaterOilburner.SLOT_FLUID_ID,
                        44,
                        71,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 121);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = 3;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityHeaterOilburner.SLOT_FLUID_ID,
                                BlockEntityHeaterOilburner.SLOT_FLUID_ID + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            BlockEntityHeaterOilburner.SLOT_FLUID_IN,
                            BlockEntityHeaterOilburner.SLOT_FLUID_IN + 1,
                            false);
                });
    }
}

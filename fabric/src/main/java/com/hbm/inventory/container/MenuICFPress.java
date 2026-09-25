// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityICFPress;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuICFPress extends BlockEntityMenu<BlockEntityICFPress> {

    public MenuICFPress(int containerId, Inventory playerInv, BlockEntityICFPress be) {
        this(containerId, playerInv, be, be);
    }

    private MenuICFPress(
            int containerId, Inventory playerInv, Container container, BlockEntityICFPress be) {
        super(ModMenus.MACHINE_ICF_PRESS.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityICFPress.SLOT_COUNT);

        addSlot(new Slot(container, BlockEntityICFPress.SLOT_EMPTY_PELLET, 98, 18));
        addSlot(
                new SlotFiltered(
                        container, BlockEntityICFPress.SLOT_OUTPUT, 98, 54, SlotFiltered.NONE));
        addSlot(new Slot(container, BlockEntityICFPress.SLOT_MUON_IN, 8, 18));
        addSlot(
                new SlotFiltered(
                        container, BlockEntityICFPress.SLOT_MUON_OUT, 8, 54, SlotFiltered.NONE));
        addSlot(new Slot(container, BlockEntityICFPress.SLOT_SOLID_A, 62, 54));
        addSlot(new Slot(container, BlockEntityICFPress.SLOT_SOLID_B, 134, 54));
        addSlot(new Slot(container, BlockEntityICFPress.SLOT_FLUID_ID_A, 62, 18));
        addSlot(new Slot(container, BlockEntityICFPress.SLOT_FLUID_ID_B, 134, 18));

        addStandardInventorySlots(playerInv, 8, 97);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int machineEnd = BlockEntityICFPress.SLOT_COUNT;
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (stack.is(ModItems.ICF_PELLET_EMPTY.get())) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityICFPress.SLOT_EMPTY_PELLET,
                                BlockEntityICFPress.SLOT_EMPTY_PELLET + 1,
                                false);
                    }
                    if (stack.getItem() instanceof FluidIdentifierItem) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityICFPress.SLOT_FLUID_ID_A,
                                BlockEntityICFPress.SLOT_FLUID_ID_B + 1,
                                false);
                    }
                    if (stack.is(ModItems.PARTICLE_MUON.get())) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityICFPress.SLOT_MUON_IN,
                                BlockEntityICFPress.SLOT_MUON_IN + 1,
                                false);
                    }
                    return moveItemStackTo(
                            stack,
                            BlockEntityICFPress.SLOT_SOLID_A,
                            BlockEntityICFPress.SLOT_SOLID_B + 1,
                            false);
                });
    }
}

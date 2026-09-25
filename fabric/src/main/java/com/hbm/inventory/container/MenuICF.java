// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityICF;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuICF extends BlockEntityMenu<BlockEntityICF> {

    public MenuICF(int containerId, Inventory playerInv, BlockEntityICF be) {
        this(containerId, playerInv, be, be);
    }

    private MenuICF(int containerId, Inventory playerInv, Container container, BlockEntityICF be) {
        super(ModMenus.MACHINE_ICF.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityICF.SLOT_COUNT);

        for (int i = 0; i < 5; i++) addSlot(new Slot(container, i, 80 + i * 18, 18));
        addSlot(new Slot(container, BlockEntityICF.SLOT_LOADED, 116, 54));
        for (int i = 0; i < 5; i++) {
            addSlot(
                    new SlotRecipeOutput(
                            playerInv.player,
                            container,
                            BlockEntityICF.SLOT_CATCH_START + i,
                            80 + i * 18,
                            90));
        }
        addSlot(new Slot(container, BlockEntityICF.SLOT_FLUID_ID, 44, 90));

        addStandardInventorySlots(playerInv, 44, 140);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlockEntityICF.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                BlockEntityICF.SLOT_FLUID_ID,
                                BlockEntityICF.SLOT_FLUID_ID + 1,
                                false);

                    return moveItemStackTo(
                                    stack,
                                    BlockEntityICF.SLOT_LOADED,
                                    BlockEntityICF.SLOT_LOADED + 1,
                                    false)
                            || moveItemStackTo(
                                    stack,
                                    BlockEntityICF.SLOT_MAGAZINE_START,
                                    BlockEntityICF.SLOT_MAGAZINE_END + 1,
                                    false);
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuRBMKRod extends BlockEntityMenu<BlockEntityRBMKRod> {

    public MenuRBMKRod(int containerId, Inventory playerInv, BlockEntityRBMKRod rod) {
        this(containerId, playerInv, rod, rod);
    }

    private MenuRBMKRod(
            int containerId, Inventory playerInv, Container container, BlockEntityRBMKRod rod) {
        super(ModMenus.RBMK_ROD.get(), containerId, rod, container);
        checkContainerSize(container, 1);

        addSlot(
                new Slot(container, 0, 80, 45) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof ItemRBMKRod;
                    }
                });

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public void clicked(
            int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex == 0
                && !player.getAbilities().instabuild
                && !blockEntity().coldEnoughForManual()) return;
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = 1;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}

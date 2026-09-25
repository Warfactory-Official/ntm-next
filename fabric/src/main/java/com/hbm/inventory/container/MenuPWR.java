// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.tileentity.machine.BlockEntityMachinePWRController;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuPWR extends BlockEntityMenu<BlockEntityMachinePWRController> {

    public MenuPWR(int containerId, Inventory playerInv, BlockEntityMachinePWRController be) {
        this(containerId, playerInv, be, be);
    }

    private MenuPWR(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachinePWRController be) {
        super(ModMenus.PWR_CONTROLLER.get(), containerId, be, container);
        checkContainerSize(container, 3);

        addSlot(
                new Slot(container, 0, 53, 5) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof ItemPWRFuel;
                    }
                });
        addSlot(
                new Slot(container, 1, 89, 32) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
        addSlot(
                new Slot(container, 2, 8, 59) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof FluidIdentifierItem;
                    }
                });

        addStandardInventorySlots(playerInv, 8, 106);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = 3;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(stack, 2, 3, false);
                    if (stack.getItem() instanceof ItemPWRFuel)
                        return moveItemStackTo(stack, 0, 1, false);
                    return false;
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKStorage;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuRBMKStorage extends BlockEntityMenu<BlockEntityRBMKStorage> {

    public MenuRBMKStorage(int containerId, Inventory playerInv, BlockEntityRBMKStorage be) {
        this(containerId, playerInv, be, be);
    }

    private MenuRBMKStorage(
            int containerId, Inventory playerInv, Container container, BlockEntityRBMKStorage be) {
        super(ModMenus.RBMK_STORAGE.get(), containerId, be, container);
        checkContainerSize(container, 12);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                addSlot(
                        new Slot(container, i + j * 3, 32 + 32 * j, 29 + 16 * i) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return stack.getItem() instanceof ItemRBMKRod;
                            }
                        });
            }
        }

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = 12;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}

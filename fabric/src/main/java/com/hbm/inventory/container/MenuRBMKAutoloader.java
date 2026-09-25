// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuRBMKAutoloader extends BlockEntityMenu<BlockEntityRBMKAutoloader> {

    public MenuRBMKAutoloader(int containerId, Inventory playerInv, BlockEntityRBMKAutoloader be) {
        this(containerId, playerInv, be, be);
    }

    private MenuRBMKAutoloader(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityRBMKAutoloader be) {
        super(ModMenus.RBMK_AUTOLOADER.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityRBMKAutoloader.SLOTS);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(SlotFiltered.gated(container, j + i * 3, 17 + 18 * j, 18 + 18 * i));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(
                        new Slot(
                                container,
                                BlockEntityRBMKAutoloader.OUTPUT_START + j + i * 3,
                                107 + 18 * j,
                                18 + 18 * i) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return false;
                            }
                        });
            }
        }

        addStandardInventorySlots(playerInv, 8, 100);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BlockEntityRBMKAutoloader.SLOTS;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, BlockEntityRBMKAutoloader.OUTPUT_START, false);
                });
    }
}

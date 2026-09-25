// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityCrucible;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuCrucible extends BlockEntityMenu<BlockEntityCrucible> {

    public MenuCrucible(int containerId, Inventory playerInv, BlockEntityCrucible be) {
        this(containerId, playerInv, be, be);
    }

    private MenuCrucible(
            int containerId, Inventory playerInv, Container container, BlockEntityCrucible be) {
        super(ModMenus.MACHINE_CRUCIBLE.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityCrucible.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(
                        new Slot(container, j + i * 3 + 1, 107 + j * 18, 18 + i * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return container.canPlaceItem(getContainerSlot(), stack);
                            }
                        });
            }
        }

        addStandardInventorySlots(playerInv, 8, 132);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityCrucible.SLOT_COUNT);
    }
}

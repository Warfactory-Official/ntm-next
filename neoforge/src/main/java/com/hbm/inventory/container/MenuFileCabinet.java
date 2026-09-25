// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.storage.BlockEntityFileCabinet;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuFileCabinet extends BlockEntityMenu<BlockEntityFileCabinet> {

    private static final int CABINET_SLOTS = 8;

    public MenuFileCabinet(int containerId, Inventory playerInv, BlockEntityFileCabinet cabinet) {
        super(ModMenus.FILE_CABINET.get(), containerId, cabinet);
        checkContainerSize(cabinet, CABINET_SLOTS);
        container().startOpen(playerInv.player);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new Slot(container(), col + row * 4, 53 + col * 18, 18 + row * 36));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 88 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 146));
        }
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
                    int invEnd = slots.size();

                    if (index < CABINET_SLOTS)
                        return moveItemStackTo(stack, CABINET_SLOTS, invEnd, true);
                    return moveItemStackTo(stack, 0, CABINET_SLOTS, false);
                });
    }
}

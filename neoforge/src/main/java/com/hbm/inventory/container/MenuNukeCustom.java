// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.bomb.BlockEntityNukeCustom;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuNukeCustom extends BlockEntityMenu<BlockEntityNukeCustom> {

    private static final int PLAYER_DROP = 56;

    public MenuNukeCustom(int containerId, Inventory playerInv, BlockEntityNukeCustom bomb) {
        super(ModMenus.NUKE_CUSTOM.get(), containerId, bomb);
        checkContainerSize(bomb, BlockEntityNukeCustom.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container(), col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                playerInv,
                                col + row * 9 + 9,
                                8 + col * 18,
                                84 + row * 18 + PLAYER_DROP));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142 + PLAYER_DROP));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int hold = BlockEntityNukeCustom.SLOT_COUNT;
                    if (index < hold) return moveItemStackTo(stack, hold, slots.size(), true);

                    return moveItemStackTo(stack, 0, hold, true);
                });
    }
}

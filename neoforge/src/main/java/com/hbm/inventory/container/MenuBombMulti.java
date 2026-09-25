// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.bomb.BlockEntityBombMulti;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MenuBombMulti extends BlockEntityMenu<BlockEntityBombMulti> {

    private static final int[] COLUMNS = {44, 62, 80};

    public MenuBombMulti(int containerId, Inventory playerInv, BlockEntityBombMulti bomb) {
        super(ModMenus.BOMB_MULTI.get(), containerId, bomb);
        checkContainerSize(bomb, BlockEntityBombMulti.SLOT_COUNT);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                int x = COLUMNS[col];
                int y = 26 + row * 18;

                addSlot(
                        col == 2
                                ? new SlotFiltered(
                                        container(),
                                        index,
                                        x,
                                        y,
                                        stack ->
                                                BlockEntityBombMulti.Payload.of(stack)
                                                        != BlockEntityBombMulti.Payload.NONE)
                                : new SlotFiltered(
                                        container(), index, x, y, stack -> stack.is(Items.TNT)));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index >= BlockEntityBombMulti.SLOT_COUNT) return false;
                    return moveItemStackTo(
                            stack, BlockEntityBombMulti.SLOT_COUNT, slots.size(), true);
                });
    }
}

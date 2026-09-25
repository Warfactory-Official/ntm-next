// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityLaunchpadSoyuz;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuLaunchpadSoyuz extends BlockEntityMenu<BlockEntityLaunchpadSoyuz> {

    public MenuLaunchpadSoyuz(int containerId, Inventory playerInv, BlockEntityLaunchpadSoyuz pad) {
        super(ModMenus.LAUNCHPAD_SOYUZ.get(), containerId, pad);
        checkContainerSize(pad, BlockEntityLaunchpadSoyuz.SLOT_COUNT);

        addSlot(new ValidatedSlot(pad, 0, 98, 80));
        addSlot(new ValidatedSlot(pad, 1, 80, 80));
        addSlot(new ValidatedSlot(pad, 2, 98, 26));
        addSlot(new ValidatedSlot(pad, 3, 80, 26));
        addSlot(new Slot(container(), 4, 152, 98));
        addSlot(new Slot(container(), 5, 152, 116));
        addSlot(new Slot(container(), 6, 170, 98));
        addSlot(new Slot(container(), 7, 170, 116));
        addSlot(new Slot(container(), 8, 134, 98));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new ValidatedSlot(pad, col + row * 6 + 9, 44 - row * 18, 26 + col * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 17 + col * 18, 162 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 17 + col * 18, 220));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < BlockEntityLaunchpadSoyuz.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, BlockEntityLaunchpadSoyuz.SLOT_COUNT, slots.size(), true);
                    }
                    return moveItemStackTo(stack, 0, BlockEntityLaunchpadSoyuz.SLOT_COUNT, false);
                });
    }

    private static final class ValidatedSlot extends Slot {

        private final BlockEntityLaunchpadSoyuz pad;

        private ValidatedSlot(BlockEntityLaunchpadSoyuz pad, int index, int x, int y) {
            super(pad, index, x, y);
            this.pad = pad;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return pad.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return Math.max(pad.getMaxStackSize(), hasItem() ? getItem().getCount() : 1);
        }
    }
}

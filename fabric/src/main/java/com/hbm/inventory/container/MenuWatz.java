// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.BlockEntityWatz;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuWatz extends BlockEntityMenu<BlockEntityWatz> {

    public MenuWatz(int containerId, Inventory playerInv, BlockEntityWatz be) {
        this(containerId, playerInv, be, be);
    }

    private MenuWatz(
            int containerId, Inventory playerInv, Container container, BlockEntityWatz be) {
        super(ModMenus.WATZ.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityWatz.SLOT_COUNT);

        int index = 0;
        for (int j = 0; j < 6; j++) {
            for (int i = 0; i < 6; i++) {
                if (i + j > 1 && i + j < 9 && 5 - i + j > 1 && i + 5 - j > 1) {
                    int slot = index;

                    addSlot(
                            new SlotFiltered(
                                    container,
                                    slot,
                                    17 + i * 18,
                                    8 + j * 18,
                                    stack -> container.canPlaceItem(slot, stack)));
                    index++;
                }
            }
        }

        addStandardInventorySlots(playerInv, 8, 147);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int invEnd = slots.size();

                    if (index < BlockEntityWatz.SLOT_COUNT)
                        return moveItemStackTo(stack, BlockEntityWatz.SLOT_COUNT, invEnd, true);
                    return moveItemStackTo(stack, 0, BlockEntityWatz.SLOT_COUNT, false);
                });
    }
}

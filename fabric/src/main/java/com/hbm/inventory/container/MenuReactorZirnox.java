// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.tileentity.machine.BlockEntityReactorZirnox;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuReactorZirnox extends BlockEntityMenu<BlockEntityReactorZirnox> {

    private static final int SLOT_COUNT = 28;

    private static final int[] ROD_X = {
        26, 62, 98, 8, 44, 80, 116, 26, 62, 98, 8, 44, 80, 116, 26, 62, 98, 8, 44, 80, 116, 26, 62,
        98
    };
    private static final int[] ROD_Y = {
        16, 16, 16, 34, 34, 34, 34, 52, 52, 52, 70, 70, 70, 70, 88, 88, 88, 106, 106, 106, 106, 124,
        124, 124
    };

    public MenuReactorZirnox(int containerId, Inventory playerInv, BlockEntityReactorZirnox be) {
        this(containerId, playerInv, be, be);
    }

    private MenuReactorZirnox(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityReactorZirnox be) {
        super(ModMenus.REACTOR_ZIRNOX.get(), containerId, be, container);
        checkContainerSize(container, SLOT_COUNT);

        for (int i = 0; i < 24; i++) {
            addSlot(
                    new SlotFiltered(
                            container,
                            i,
                            ROD_X[i],
                            ROD_Y[i],
                            s -> s.getItem() instanceof ItemZirnoxRod));
        }
        addSlot(new Slot(container, 24, 143, 124));
        addSlot(new SlotFiltered(container, 26, 143, 142, SlotFiltered.NONE));
        addSlot(new Slot(container, 25, 179, 124));
        addSlot(new SlotFiltered(container, 27, 179, 142, SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 174);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int invEnd = slots.size();

                    if (index < SLOT_COUNT) return moveItemStackTo(stack, SLOT_COUNT, invEnd, true);
                    if (stack.getItem() instanceof ItemZirnoxRod)
                        return moveItemStackTo(stack, 0, 24, true);
                    if (blockEntity().carbonDioxide.containerContent(stack) > 0) {
                        return moveItemStackTo(stack, 24, 25, false);
                    }
                    if (blockEntity().water.containerContent(stack) > 0)
                        return moveItemStackTo(stack, 26, 27, false);
                    return false;
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.entity.train.TrainCargoTramTrailer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuTrainCargoTramTrailer extends AbstractContainerMenu {

    private final Container container;

    public MenuTrainCargoTramTrailer(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(TrainCargoTramTrailer.SLOT_COUNT));
    }

    public MenuTrainCargoTramTrailer(
            int containerId, Inventory playerInv, TrainCargoTramTrailer train) {
        this(containerId, playerInv, (Container) train);
    }

    private MenuTrainCargoTramTrailer(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.TRAIN_CARGO_TRAM_TRAILER.get(), containerId);
        checkContainerSize(container, TrainCargoTramTrailer.SLOT_COUNT);
        this.container = container;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 198));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int cargo = TrainCargoTramTrailer.SLOT_COUNT;

        if (index < cargo) {
            if (!moveItemStackTo(stack, cargo, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, cargo, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}

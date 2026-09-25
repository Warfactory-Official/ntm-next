// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.entity.train.TrainCargoTram;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuTrainCargoTram extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public MenuTrainCargoTram(int containerId, Inventory playerInv) {
        this(
                containerId,
                playerInv,
                new SimpleContainer(TrainCargoTram.SLOT_COUNT),
                new SimpleContainerData(1));
    }

    public MenuTrainCargoTram(int containerId, Inventory playerInv, TrainCargoTram train) {
        this(
                containerId,
                playerInv,
                train,
                new ContainerData() {
                    @Override
                    public int get(int index) {
                        return train.getPower();
                    }

                    @Override
                    public void set(int index, int value) {
                        train.setPower(value);
                    }

                    @Override
                    public int getCount() {
                        return 1;
                    }
                });
    }

    private MenuTrainCargoTram(
            int containerId, Inventory playerInv, Container container, ContainerData data) {
        super(ModMenus.TRAIN_CARGO_TRAM.get(), containerId);
        checkContainerSize(container, TrainCargoTram.SLOT_COUNT);
        this.container = container;
        this.data = data;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 7; col++) {
                addSlot(new Slot(container, row * 7 + col, 8 + col * 18, 18 + row * 18));
            }
        }
        addSlot(new Slot(container, TrainCargoTram.SLOT_CHARGE, 152, 72));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 180));
        }

        addDataSlots(data);
    }

    public int getPower() {
        return this.data.get(0);
    }

    public int getMaxPower() {
        return getPowerConsumption() * 100;
    }

    public int getPowerConsumption() {
        return 10;
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
        int cargo = TrainCargoTram.SLOT_COUNT;

        if (index < cargo) {
            if (!moveItemStackTo(stack, cargo, slots.size(), true)) return ItemStack.EMPTY;
        } else if (IBatteryItem.isBattery(original)) {
            if (!moveItemStackTo(stack, TrainCargoTram.SLOT_CHARGE, cargo, false))
                return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, TrainCargoTram.SLOT_CHARGE, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}

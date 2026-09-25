// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability.port;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record PortContainerView(WorldlyContainer owner, ItemPort access)
        implements WorldlyContainer {

    @Override
    public int[] getSlotsForFace(Direction side) {
        return access.slots();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return access.canInsert(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return access.canExtract(slot, stack);
    }

    @Override
    public int getContainerSize() {
        return owner.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return owner.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return owner.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return owner.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return owner.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        owner.setItem(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return owner.getMaxStackSize();
    }

    @Override
    public void setChanged() {
        owner.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return owner.stillValid(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return owner.canPlaceItem(slot, stack);
    }

    @Override
    public void clearContent() {
        owner.clearContent();
    }
}

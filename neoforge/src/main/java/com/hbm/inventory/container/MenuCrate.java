// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.storage.BlockEntityCrate;
import com.hbm.tileentity.machine.storage.CrateType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuCrate extends NtmContainerMenu {

    private final CrateType type;

    public MenuCrate(int containerId, Inventory playerInv, CrateType type) {
        this(containerId, playerInv, type, new SimpleContainer(type.slots));
    }

    public MenuCrate(int containerId, Inventory playerInv, BlockEntityCrate crate) {
        this(containerId, playerInv, crate.crateType, crate);
    }

    public MenuCrate(int containerId, Inventory playerInv, CrateType type, Container container) {
        super(ModMenus.crateMenuType(type).get(), containerId, container);
        checkContainerSize(container, type.slots);
        this.type = type;

        for (int row = 0; row < type.rows; row++) {
            for (int col = 0; col < type.columns; col++) {
                int slot = col + row * type.columns;
                addSlot(
                        new ShulkerBoxSlot(
                                container, slot, type.crateX + col * 18, type.crateY + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                playerInv,
                                col + row * 9 + 9,
                                type.playerInventoryX + col * 18,
                                type.playerInventoryY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, type.playerInventoryX + col * 18, type.hotbarY));
        }
        container.startOpen(playerInv.player);
    }

    public CrateType getCrateType() {
        return type;
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
                    int crateSlots = type.slots;
                    int invEnd = slots.size();
                    if (index < crateSlots)
                        return moveItemStackToFiltered(stack, crateSlots, invEnd, true);
                    return moveItemStackToFiltered(stack, 0, crateSlots, false);
                });
    }
}

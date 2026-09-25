// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.entity.cart.EntityMinecartDestroyer;
import com.hbm.inventory.slot.SlotPattern;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuCartDestroyer extends NtmContainerMenu {

    public MenuCartDestroyer(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(EntityMinecartDestroyer.FILTER_SLOTS));
    }

    public MenuCartDestroyer(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.CART_DESTROYER.get(), containerId, container);
        checkContainerSize(container, EntityMinecartDestroyer.FILTER_SLOTS);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new SlotPattern(container, j + i * 3, 10 + j * 18, 17 + i * 18));
                addSlot(new SlotPattern(container, j + i * 3 + 9, 114 + j * 18, 17 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < EntityMinecartDestroyer.FILTER_SLOTS) {
            slots.get(slotId).set(getCarried());
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

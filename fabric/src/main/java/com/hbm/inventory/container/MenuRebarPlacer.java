// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotPattern;
import com.hbm.platform.Services;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuRebarPlacer extends NtmContainerMenu {

    public MenuRebarPlacer(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(1));
    }

    public MenuRebarPlacer(int containerId, Inventory playerInv, Container pattern) {
        super(ModMenus.REBAR_PLACER.get(), containerId, pattern);
        addSlot(new SlotPattern(pattern, 0, 53, 36));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 100 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 158));
        }
    }

    public Slot pattern() {
        return getSlot(0);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId != 0) {
            super.clicked(slotId, button, input, player);
            return;
        }
        if (Services.PLATFORM.canFitInsideContainerItems(getCarried())) pattern().set(getCarried());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

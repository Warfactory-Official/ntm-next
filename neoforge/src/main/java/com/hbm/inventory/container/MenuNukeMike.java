// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityNukeMike;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuNukeMike extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityNukeMike.SLOT_COUNT;

    public MenuNukeMike(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuNukeMike(int containerId, Inventory playerInv, BlockEntityNukeMike nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private MenuNukeMike(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_MIKE.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, 0, 26, 83));
        addSlot(new SlotComponent(container, 1, 26, 101));
        addSlot(new SlotComponent(container, 2, 44, 83));
        addSlot(new SlotComponent(container, 3, 44, 101));
        addSlot(new SlotComponent(container, 4, 39, 35));
        addSlot(new SlotComponent(container, 5, 98, 91));
        addSlot(new SlotComponent(container, 6, 116, 91));
        addSlot(new SlotComponent(container, 7, 134, 91));
        addStandardInventorySlots(playerInv, 8, 135);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityNukeMike.isReady(container());
    }

    public boolean isFilled() {
        return BlockEntityNukeMike.isFilled(container());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index < MACHINE_SLOTS
                                && moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true));
    }
}

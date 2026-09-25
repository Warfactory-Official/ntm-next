// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityNukeTsar;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuNukeTsar extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityNukeTsar.SLOT_COUNT;

    public MenuNukeTsar(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuNukeTsar(int containerId, Inventory playerInv, BlockEntityNukeTsar nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private MenuNukeTsar(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_TSAR.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, 0, 48, 101));
        addSlot(new SlotComponent(container, 1, 66, 101));
        addSlot(new SlotComponent(container, 2, 84, 101));
        addSlot(new SlotComponent(container, 3, 102, 101));
        addSlot(new SlotComponent(container, 4, 55, 51));
        addSlot(new SlotComponent(container, 5, 138, 101));
        addStandardInventorySlots(playerInv, 48, 151);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityNukeTsar.isReady(container());
    }

    public boolean isFilled() {
        return BlockEntityNukeTsar.isFilled(container());
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

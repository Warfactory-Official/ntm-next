// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityNukeFleija;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuNukeFleija extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityNukeFleija.SLOT_COUNT;

    public MenuNukeFleija(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuNukeFleija(int containerId, Inventory playerInv, BlockEntityNukeFleija nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private MenuNukeFleija(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_FLEIJA.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, 0, 8, 36));
        addSlot(new SlotComponent(container, 1, 152, 36));
        addSlot(new SlotComponent(container, 2, 44, 18));
        addSlot(new SlotComponent(container, 3, 44, 36));
        addSlot(new SlotComponent(container, 4, 44, 54));
        addSlot(new SlotComponent(container, 5, 80, 18));
        addSlot(new SlotComponent(container, 6, 98, 18));
        addSlot(new SlotComponent(container, 7, 80, 36));
        addSlot(new SlotComponent(container, 8, 98, 36));
        addSlot(new SlotComponent(container, 9, 80, 54));
        addSlot(new SlotComponent(container, 10, 98, 54));
        addStandardInventorySlots(playerInv, 8, 140);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityNukeFleija.isReady(container());
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

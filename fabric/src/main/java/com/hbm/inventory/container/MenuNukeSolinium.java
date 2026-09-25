// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityNukeSolinium;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuNukeSolinium extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityNukeSolinium.SLOT_COUNT;

    public MenuNukeSolinium(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuNukeSolinium(int containerId, Inventory playerInv, BlockEntityNukeSolinium nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private MenuNukeSolinium(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_SOLINIUM.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, 0, 26, 18));
        addSlot(new SlotComponent(container, 1, 53, 18));
        addSlot(new SlotComponent(container, 2, 107, 18));
        addSlot(new SlotComponent(container, 3, 134, 18));
        addSlot(new SlotComponent(container, 4, 80, 36));
        addSlot(new SlotComponent(container, 5, 26, 54));
        addSlot(new SlotComponent(container, 6, 53, 54));
        addSlot(new SlotComponent(container, 7, 107, 54));
        addSlot(new SlotComponent(container, 8, 134, 54));
        addStandardInventorySlots(playerInv, 8, 140);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityNukeSolinium.isReady(container());
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

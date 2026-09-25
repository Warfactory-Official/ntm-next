// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityNukeN2;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuNukeN2 extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityNukeN2.SLOT_COUNT;

    public MenuNukeN2(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuNukeN2(int containerId, Inventory playerInv, BlockEntityNukeN2 nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private MenuNukeN2(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_N2.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, 0, 98, 36));
        addSlot(new SlotComponent(container, 1, 116, 36));
        addSlot(new SlotComponent(container, 2, 134, 36));
        addSlot(new SlotComponent(container, 3, 98, 54));
        addSlot(new SlotComponent(container, 4, 116, 54));
        addSlot(new SlotComponent(container, 5, 134, 54));
        addSlot(new SlotComponent(container, 6, 98, 72));
        addSlot(new SlotComponent(container, 7, 116, 72));
        addSlot(new SlotComponent(container, 8, 134, 72));
        addSlot(new SlotComponent(container, 9, 98, 90));
        addSlot(new SlotComponent(container, 10, 116, 90));
        addSlot(new SlotComponent(container, 11, 134, 90));
        addStandardInventorySlots(playerInv, 8, 140);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityNukeN2.isReady(container());
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

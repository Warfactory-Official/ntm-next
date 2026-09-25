// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityNukeGadget;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuNukeGadget extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityNukeGadget.SLOT_COUNT;

    public MenuNukeGadget(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuNukeGadget(int containerId, Inventory playerInv, BlockEntityNukeGadget nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private MenuNukeGadget(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_GADGET.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, 0, 26, 35));
        addSlot(new SlotComponent(container, 1, 8, 17));
        addSlot(new SlotComponent(container, 2, 44, 17));
        addSlot(new SlotComponent(container, 3, 8, 53));
        addSlot(new SlotComponent(container, 4, 44, 53));
        addSlot(new SlotComponent(container, 5, 98, 35));
        addStandardInventorySlots(playerInv, 8, 84);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityNukeGadget.isReady(container());
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

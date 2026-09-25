// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityNukeMan;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuNukeMan extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityNukeMan.SLOT_COUNT;

    public MenuNukeMan(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuNukeMan(int containerId, Inventory playerInv, BlockEntityNukeMan nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private MenuNukeMan(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_MAN.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, BlockEntityNukeMan.SLOT_IGNITER, 26, 35));
        addSlot(new SlotComponent(container, BlockEntityNukeMan.SLOT_LENS_1, 8, 17));
        addSlot(new SlotComponent(container, BlockEntityNukeMan.SLOT_LENS_2, 44, 17));
        addSlot(new SlotComponent(container, BlockEntityNukeMan.SLOT_LENS_3, 8, 53));
        addSlot(new SlotComponent(container, BlockEntityNukeMan.SLOT_LENS_4, 44, 53));
        addSlot(new SlotComponent(container, BlockEntityNukeMan.SLOT_CORE, 98, 35));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityNukeMan.isReady(container());
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

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityNukePrototype;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuNukePrototype extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityNukePrototype.SLOT_COUNT;

    public MenuNukePrototype(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuNukePrototype(int containerId, Inventory playerInv, BlockEntityNukePrototype nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private MenuNukePrototype(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_PROTOTYPE.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, 0, 8, 35));
        addSlot(new SlotComponent(container, 1, 26, 35));
        addSlot(new SlotComponent(container, 2, 44, 26));
        addSlot(new SlotComponent(container, 3, 44, 44));
        addSlot(new SlotComponent(container, 4, 62, 26));
        addSlot(new SlotComponent(container, 5, 62, 44));
        addSlot(new SlotComponent(container, 6, 80, 26));
        addSlot(new SlotComponent(container, 7, 80, 44));
        addSlot(new SlotComponent(container, 8, 98, 26));
        addSlot(new SlotComponent(container, 9, 98, 44));
        addSlot(new SlotComponent(container, 10, 116, 26));
        addSlot(new SlotComponent(container, 11, 116, 44));
        addSlot(new SlotComponent(container, 12, 134, 35));
        addSlot(new SlotComponent(container, 13, 152, 35));
        addStandardInventorySlots(playerInv, 8, 84);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityNukePrototype.isReady(container());
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

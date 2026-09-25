// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityNukeBoy;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuNukeBoy extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityNukeBoy.SLOT_COUNT;

    public MenuNukeBoy(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuNukeBoy(int containerId, Inventory playerInv, BlockEntityNukeBoy nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private MenuNukeBoy(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_BOY.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, 0, 26, 36));
        addSlot(new SlotComponent(container, 1, 44, 36));
        addSlot(new SlotComponent(container, 2, 62, 36));
        addSlot(new SlotComponent(container, 3, 80, 36));
        addSlot(new SlotComponent(container, 4, 98, 36));
        addStandardInventorySlots(playerInv, 8, 140);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityNukeBoy.isReady(container());
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

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.ItemSatelliteChip;
import com.hbm.tileentity.machine.BlockEntityMachineSatDock;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuSatDock extends BlockEntityMenu<BlockEntityMachineSatDock> {

    private static final int SLOTS = BlockEntityMachineSatDock.SLOT_COUNT;
    private static final int CARGO = BlockEntityMachineSatDock.CARGO_SLOTS;
    private static final int COLUMNS = 5;

    public MenuSatDock(int containerId, Inventory playerInv, BlockEntityMachineSatDock dock) {
        this(containerId, playerInv, dock, dock);
    }

    private MenuSatDock(
            int containerId,
            Inventory playerInv,
            Container container,
            BlockEntityMachineSatDock dock) {
        super(ModMenus.SAT_DOCK.get(), containerId, dock, container);
        checkContainerSize(container, SLOTS);

        for (int i = 0; i < CARGO; i++) {
            addSlot(
                    new SlotFiltered(
                            container,
                            i,
                            71 + (i % COLUMNS) * 18,
                            18 + (i / COLUMNS) * 18,
                            SlotFiltered.NONE));
        }
        addSlot(
                new SlotFiltered(
                        container,
                        BlockEntityMachineSatDock.SLOT_CHIP,
                        26,
                        36,
                        stack -> stack.getItem() instanceof ItemSatelliteChip));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index < SLOTS
                                ? moveItemStackTo(stack, SLOTS, slots.size(), true)
                                : moveItemStackTo(stack, 0, CARGO, false));
    }
}

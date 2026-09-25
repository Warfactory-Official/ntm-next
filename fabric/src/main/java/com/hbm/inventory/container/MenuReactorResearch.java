// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntityReactorResearch;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuReactorResearch extends BlockEntityMenu<BlockEntityReactorResearch> {

    private static final int[][] SLOT_POSITIONS = {
        {95, 22}, {131, 22}, {77, 40}, {113, 40}, {149, 40}, {95, 58}, {131, 58}, {77, 76},
        {113, 76}, {149, 76}, {95, 94}, {131, 94}
    };

    private final SyncedData data;

    public MenuReactorResearch(
            int containerId, Inventory playerInv, BlockEntityReactorResearch be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BlockEntityReactorResearch.class)
                        : SyncedData.of(be),
                be);
    }

    private MenuReactorResearch(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BlockEntityReactorResearch be) {
        super(ModMenus.REACTOR_RESEARCH.get(), containerId, be, container);
        checkContainerSize(container, BlockEntityReactorResearch.SLOT_COUNT);
        this.data = data;

        for (int i = 0; i < SLOT_POSITIONS.length; i++) {
            addSlot(new Slot(container, i, SLOT_POSITIONS[i][0], SLOT_POSITIONS[i][1]));
        }

        addStandardInventorySlots(playerInv, 8, 140);
        addDataSlots(data);
    }

    public int getHeat() {
        return (int) data.get("heat");
    }

    public int getTotalFlux() {
        return (int) data.get("totalFlux");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityReactorResearch.SLOT_COUNT);
    }
}

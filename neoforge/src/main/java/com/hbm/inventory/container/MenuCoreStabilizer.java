// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.BlockEntityCoreStabilizer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuCoreStabilizer extends BlockEntityMenu<BlockEntityCoreStabilizer> {

    public MenuCoreStabilizer(int containerId, Inventory playerInv, BlockEntityCoreStabilizer be) {
        super(ModMenus.CORE_STABILIZER.get(), containerId, be);
        checkContainerSize(be, BlockEntityCoreStabilizer.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityCoreStabilizer.SLOT_LENS,
                        80,
                        17,
                        s -> be.canPlaceItem(BlockEntityCoreStabilizer.SLOT_LENS, s)));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityCoreStabilizer.SLOT_COUNT);
    }
}

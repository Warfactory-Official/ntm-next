// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.tileentity.network.BlockEntityCraneUnboxer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuCraneUnboxer extends BlockEntityMenu<BlockEntityCraneUnboxer> {

    public MenuCraneUnboxer(
            int containerId, Inventory playerInventory, BlockEntityCraneUnboxer be) {
        super(ModMenus.CRANE_UNBOXER.get(), containerId, be);
        checkContainerSize(be, BlockEntityCraneUnboxer.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                addSlot(new Slot(be, j + i * 7, 8 + j * 18, 17 + i * 18));
            }
        }

        addSlot(new SlotUpgrade(be, BlockEntityCraneUnboxer.SLOT_UPGRADE_STACK, 152, 23));
        addSlot(new SlotUpgrade(be, BlockEntityCraneUnboxer.SLOT_UPGRADE_EJECTOR, 152, 47));

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityCraneUnboxer.SLOT_COUNT);
    }
}

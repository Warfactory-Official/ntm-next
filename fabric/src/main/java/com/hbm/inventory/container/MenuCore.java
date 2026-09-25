// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.BlockEntityCore;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuCore extends BlockEntityMenu<BlockEntityCore> {

    public MenuCore(int containerId, Inventory playerInv, BlockEntityCore be) {
        super(ModMenus.CORE_CORE.get(), containerId, be);
        checkContainerSize(be, BlockEntityCore.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityCore.SLOT_CATALYST_A,
                        62,
                        53,
                        s -> be.canPlaceItem(BlockEntityCore.SLOT_CATALYST_A, s)));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityCore.SLOT_CORE,
                        80,
                        53,
                        s -> be.canPlaceItem(BlockEntityCore.SLOT_CORE, s)));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityCore.SLOT_CATALYST_B,
                        98,
                        53,
                        s -> be.canPlaceItem(BlockEntityCore.SLOT_CATALYST_B, s)));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityCore.SLOT_COUNT);
    }
}

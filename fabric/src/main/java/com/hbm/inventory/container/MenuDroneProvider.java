// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.network.BlockEntityDroneProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuDroneProvider extends BlockEntityMenu<BlockEntityDroneProvider> {

    public MenuDroneProvider(
            int containerId, Inventory playerInventory, BlockEntityDroneProvider provider) {
        super(ModMenus.DRONE_PROVIDER.get(), containerId, provider);
        checkContainerSize(provider, BlockEntityDroneProvider.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(provider, col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveFilteredUnsorted(player, index, BlockEntityDroneProvider.SLOT_COUNT);
    }
}

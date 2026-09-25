// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotPattern;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageMono;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuPneumoStorageMono extends BlockEntityMenu<BlockEntityPneumoStorageMono> {

    public MenuPneumoStorageMono(
            int containerId, Inventory playerInventory, BlockEntityPneumoStorageMono be) {
        super(ModMenus.PNEUMATIC_STORAGE_MONO.get(), containerId, be);
        checkContainerSize(be, BlockEntityPneumoStorageMono.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            addSlot(new SlotPattern(be, i, 8, 17 + i * 18));
        }

        addStandardInventorySlots(playerInventory, 8, 99);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= BlockEntityPneumoStorageMono.SLOT_COUNT) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        Slot slot = getSlot(slotIndex);
        if (blockEntity().amounts[slotIndex] > 0 && slot.hasItem()
                || !blockEntity().acceptsFilter(getCarried())) return;

        slot.set(getCarried());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotPattern;
import com.hbm.tileentity.network.BlockEntityCraneRouter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuCraneRouter extends BlockEntityMenu<BlockEntityCraneRouter> {

    public MenuCraneRouter(int containerId, Inventory playerInventory, BlockEntityCraneRouter be) {
        super(ModMenus.CRANE_ROUTER.get(), containerId, be);
        checkContainerSize(be, BlockEntityCraneRouter.SLOT_COUNT);

        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < 3; i++) {
                for (int k = 0; k < BlockEntityCraneRouter.FILTERS_PER_SIDE; k++) {
                    addSlot(
                            new SlotPattern(
                                    be, k + j * 15 + i * 5, 34 + k * 18 + j * 98, 17 + i * 26));
                }
            }
        }

        addStandardInventorySlots(playerInventory, 47, 119);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= BlockEntityCraneRouter.SLOT_COUNT) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        BlockEntityCraneRouter router = blockEntity();
        Slot slot = getSlot(slotIndex);

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            router.nextMode(slotIndex);
            return;
        }

        slot.set(getCarried());
        router.initPattern(slot.getItem(), slotIndex);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

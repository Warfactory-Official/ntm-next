// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.network.BlockEntityRadioTorchCounter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MenuRadioTorchCounter extends BlockEntityMenu<BlockEntityRadioTorchCounter> {

    public MenuRadioTorchCounter(
            int containerId, Inventory playerInventory, BlockEntityRadioTorchCounter counter) {
        super(ModMenus.RADIO_TORCH_COUNTER.get(), containerId, counter);
        checkContainerSize(counter, BlockEntityRadioTorchCounter.FILTER_COUNT);
        for (int i = 0; i < BlockEntityRadioTorchCounter.FILTER_COUNT; i++) {
            addSlot(new GhostSlot(counter, i, 138, 18 + 44 * i));
        }
        addStandardInventorySlots(playerInventory, 12, 156);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= BlockEntityRadioTorchCounter.FILTER_COUNT) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        BlockEntityRadioTorchCounter counter = blockEntity();
        Slot slot = getSlot(slotIndex);
        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            counter.nextMode(slotIndex);
            counter.setChanged();
            return;
        }

        ItemStack carried = getCarried();
        ItemStack pattern = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
        counter.setItem(slotIndex, pattern);
        counter.matcher.initPatternStandard(counter.getLevel(), pattern, slotIndex);
    }

    private static final class GhostSlot extends Slot {

        private GhostSlot(BlockEntityRadioTorchCounter counter, int slot, int x, int y) {
            super(counter, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}

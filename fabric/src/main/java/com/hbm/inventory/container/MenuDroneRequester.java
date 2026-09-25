// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotPattern;
import com.hbm.tileentity.network.BlockEntityDroneRequester;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuDroneRequester extends BlockEntityMenu<BlockEntityDroneRequester> {

    public MenuDroneRequester(
            int containerId, Inventory playerInventory, BlockEntityDroneRequester requester) {
        super(ModMenus.DRONE_REQUESTER.get(), containerId, requester);
        checkContainerSize(requester, BlockEntityDroneRequester.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotPattern(requester, col + row * 3, 98 + col * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = col + row * 3 + BlockEntityDroneRequester.FILTER_COUNT;
                addSlot(new Slot(requester, slot, 26 + col * 18, 17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < BlockEntityDroneRequester.FILTER_COUNT) return ItemStack.EMPTY;

        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineSlots = BlockEntityDroneRequester.SLOT_COUNT;
                    if (index < machineSlots)
                        return moveItemStackToFiltered(stack, machineSlots, slots.size(), true);
                    return moveItemStackToFiltered(
                            stack, BlockEntityDroneRequester.FILTER_COUNT, machineSlots, false);
                });
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= BlockEntityDroneRequester.FILTER_COUNT) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        BlockEntityDroneRequester requester = blockEntity();
        Slot slot = getSlot(slotIndex);

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            requester.nextMode(slotIndex);
            requester.setChanged();
            return;
        }

        ItemStack carried = getCarried();
        ItemStack pattern = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
        requester.setItem(slotIndex, pattern);
        requester.matcher.initPatternStandard(requester.getLevel(), pattern, slotIndex);
    }
}

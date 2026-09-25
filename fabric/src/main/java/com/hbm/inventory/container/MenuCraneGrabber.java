// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotPattern;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.tileentity.network.BlockEntityCraneGrabber;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuCraneGrabber extends BlockEntityMenu<BlockEntityCraneGrabber> {

    public MenuCraneGrabber(
            int containerId, Inventory playerInventory, BlockEntityCraneGrabber be) {
        super(ModMenus.CRANE_GRABBER.get(), containerId, be);
        checkContainerSize(be, BlockEntityCraneGrabber.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new SlotPattern(be, j + i * 3, 40 + j * 18, 17 + i * 18));
            }
        }

        addSlot(new SlotUpgrade(be, BlockEntityCraneGrabber.SLOT_UPGRADE_STACK, 121, 23));
        addSlot(new SlotUpgrade(be, BlockEntityCraneGrabber.SLOT_UPGRADE_EJECTOR, 121, 47));

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= BlockEntityCraneGrabber.FILTER_SLOTS) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        BlockEntityCraneGrabber grabber = blockEntity();
        Slot slot = getSlot(slotIndex);

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            grabber.nextMode(slotIndex);
            return;
        }

        slot.set(getCarried());
        grabber.matcher.initPatternStandard(grabber.getLevel(), slot.getItem(), slotIndex);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < BlockEntityCraneGrabber.FILTER_SLOTS) return ItemStack.EMPTY;

        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < BlockEntityCraneGrabber.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, BlockEntityCraneGrabber.SLOT_COUNT, slots.size(), true);
                    }
                    if (ItemMachineUpgrade.getLevel(stack, UpgradeType.STACK) > 0) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityCraneGrabber.SLOT_UPGRADE_STACK,
                                BlockEntityCraneGrabber.SLOT_UPGRADE_STACK + 1,
                                false);
                    }
                    if (ItemMachineUpgrade.getLevel(stack, UpgradeType.EJECTOR) > 0) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityCraneGrabber.SLOT_UPGRADE_EJECTOR,
                                BlockEntityCraneGrabber.SLOT_UPGRADE_EJECTOR + 1,
                                false);
                    }
                    return false;
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotPattern;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.tileentity.network.BlockEntityCraneExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuCraneExtractor extends BlockEntityMenu<BlockEntityCraneExtractor> {

    public MenuCraneExtractor(
            int containerId, Inventory playerInventory, BlockEntityCraneExtractor be) {
        super(ModMenus.CRANE_EXTRACTOR.get(), containerId, be);
        checkContainerSize(be, BlockEntityCraneExtractor.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new SlotPattern(be, j + i * 3, 71 + j * 18, 17 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(
                        new Slot(
                                be,
                                BlockEntityCraneExtractor.SLOT_BUFFER + j + i * 3,
                                8 + j * 18,
                                17 + i * 18));
            }
        }

        addSlot(new SlotUpgrade(be, BlockEntityCraneExtractor.SLOT_UPGRADE_STACK, 152, 23));
        addSlot(new SlotUpgrade(be, BlockEntityCraneExtractor.SLOT_UPGRADE_EJECTOR, 152, 47));

        addStandardInventorySlots(playerInventory, 26, 103);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= BlockEntityCraneExtractor.FILTER_SLOTS) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        BlockEntityCraneExtractor extractor = blockEntity();
        Slot slot = getSlot(slotIndex);

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            extractor.nextMode(slotIndex);
            return;
        }

        slot.set(getCarried());
        extractor.matcher.initPatternStandard(extractor.getLevel(), slot.getItem(), slotIndex);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < BlockEntityCraneExtractor.FILTER_SLOTS) return ItemStack.EMPTY;

        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < BlockEntityCraneExtractor.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, BlockEntityCraneExtractor.SLOT_COUNT, slots.size(), true);
                    }
                    if (ItemMachineUpgrade.getLevel(stack, UpgradeType.STACK) > 0) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityCraneExtractor.SLOT_UPGRADE_STACK,
                                BlockEntityCraneExtractor.SLOT_UPGRADE_STACK + 1,
                                false);
                    }
                    if (ItemMachineUpgrade.getLevel(stack, UpgradeType.EJECTOR) > 0) {
                        return moveItemStackTo(
                                stack,
                                BlockEntityCraneExtractor.SLOT_UPGRADE_EJECTOR,
                                BlockEntityCraneExtractor.SLOT_UPGRADE_EJECTOR + 1,
                                false);
                    }
                    return moveItemStackTo(
                            stack,
                            BlockEntityCraneExtractor.SLOT_BUFFER,
                            BlockEntityCraneExtractor.SLOT_COUNT,
                            false);
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotPattern;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageExporter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuPneumoStorageExporter extends BlockEntityMenu<BlockEntityPneumoStorageExporter> {

    public MenuPneumoStorageExporter(
            int containerId, Inventory playerInventory, BlockEntityPneumoStorageExporter be) {
        super(ModMenus.PNEUMATIC_STORAGE_EXPORTER.get(), containerId, be);
        checkContainerSize(be, BlockEntityPneumoStorageExporter.SLOT_COUNT);

        for (int i = 0; i < 9; i++) {
            addSlot(new SlotPattern(be, i, 17 + (i % 3) * 18, 17 + (i / 3) * 18).allowStackSize());
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(
                        new SlotFiltered(
                                be,
                                9 + col + row * 3,
                                80 + col * 18,
                                17 + row * 18,
                                SlotFiltered.NONE));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= 9) {
            super.clicked(slotIndex, button, input, player);
            return;
        }
        getSlot(slotIndex).set(getCarried());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index >= BlockEntityPneumoStorageExporter.SLOT_COUNT) return false;
                    int machineEnd = BlockEntityPneumoStorageExporter.SLOT_COUNT;
                    return moveItemStackTo(stack, machineEnd, slots.size(), true);
                });
    }
}

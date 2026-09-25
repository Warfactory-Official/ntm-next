// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.storage.BlockEntitySoyuzCapsule;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuSoyuzCapsule extends BlockEntityMenu<BlockEntitySoyuzCapsule> {

    public MenuSoyuzCapsule(int containerId, Inventory playerInv, BlockEntitySoyuzCapsule capsule) {
        super(ModMenus.SOYUZ_CAPSULE.get(), containerId, capsule);
        checkContainerSize(capsule, BlockEntitySoyuzCapsule.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new Slot(container(), col + row * 6, 62 + col * 18, 18 + row * 18));
            }
        }
        addSlot(new Slot(container(), BlockEntitySoyuzCapsule.SLOT_ROCKET, 17, 36));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 162));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int capsule = BlockEntitySoyuzCapsule.SLOT_COUNT;
                    if (index < capsule) return moveItemStackTo(stack, capsule, slots.size(), true);
                    return moveItemStackTo(stack, 0, capsule, false);
                });
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.machine.BlockEntitySoyuzLauncher;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Deprecated
public class MenuSoyuzLauncher extends BlockEntityMenu<BlockEntitySoyuzLauncher> {

    private static final int MACHINE_END = BlockEntitySoyuzLauncher.SLOT_COUNT;

    public MenuSoyuzLauncher(
            int containerId, Inventory playerInv, BlockEntitySoyuzLauncher launcher) {
        super(ModMenus.SOYUZ_LAUNCHER.get(), containerId, launcher);
        checkContainerSize(launcher, BlockEntitySoyuzLauncher.SLOT_COUNT);

        addSlot(new Slot(container(), BlockEntitySoyuzLauncher.SLOT_ROCKET, 98, 80));
        addSlot(new Slot(container(), BlockEntitySoyuzLauncher.SLOT_DESIGNATOR, 80, 80));
        addSlot(new Slot(container(), BlockEntitySoyuzLauncher.SLOT_SATELLITE, 98, 26));
        addSlot(new Slot(container(), BlockEntitySoyuzLauncher.SLOT_MODULE, 80, 26));
        addSlot(new Slot(container(), BlockEntitySoyuzLauncher.SLOT_FUEL_IN, 152, 98));
        addSlot(new Slot(container(), BlockEntitySoyuzLauncher.SLOT_FUEL_OUT, 152, 116));
        addSlot(new Slot(container(), BlockEntitySoyuzLauncher.SLOT_OXIDIZER_IN, 170, 98));
        addSlot(new Slot(container(), BlockEntitySoyuzLauncher.SLOT_OXIDIZER_OUT, 170, 116));
        addSlot(new Slot(container(), BlockEntitySoyuzLauncher.SLOT_BATTERY, 134, 98));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(
                        new Slot(
                                container(),
                                col + row * 6 + BlockEntitySoyuzLauncher.SLOT_CARGO,
                                44 - row * 18,
                                26 + col * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 17 + col * 18, 162 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 17 + col * 18, 220));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < MACHINE_END) {
                        return moveItemStackTo(
                                stack, BlockEntitySoyuzLauncher.SLOT_CARGO, slots.size(), true);
                    }
                    return moveItemStackTo(
                            stack,
                            BlockEntitySoyuzLauncher.SLOT_ROCKET,
                            BlockEntitySoyuzLauncher.SLOT_ROCKET + 1,
                            false);
                });
    }
}

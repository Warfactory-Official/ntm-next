// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotPattern;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTube;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuPneumoTube extends BlockEntityMenu<BlockEntityPneumoTube> {

    public MenuPneumoTube(int containerId, Inventory playerInventory, BlockEntityPneumoTube be) {
        super(ModMenus.PNEUMATIC_TUBE.get(), containerId, be);
        checkContainerSize(be, BlockEntityPneumoTube.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                addSlot(new SlotPattern(be, i * 5 + j, 35 + j * 18, 17 + i * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= BlockEntityPneumoTube.SLOT_COUNT) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        BlockEntityPneumoTube tube = blockEntity();
        Slot slot = getSlot(slotIndex);

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            tube.nextMode(slotIndex);
            return;
        }

        slot.set(getCarried());
        tube.initPattern(slot.getItem(), slotIndex);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

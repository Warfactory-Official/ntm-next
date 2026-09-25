// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.network.BlockEntityDroneCrate;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuDroneCrate extends BlockEntityMenu<BlockEntityDroneCrate> {

    public MenuDroneCrate(int containerId, Inventory playerInventory, BlockEntityDroneCrate crate) {
        super(ModMenus.DRONE_CRATE.get(), containerId, crate);
        checkContainerSize(crate, BlockEntityDroneCrate.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new Slot(crate, col + row * 6, 8 + col * 18, 17 + row * 18));
            }
        }

        addSlot(
                new SlotFiltered(
                        crate,
                        BlockEntityDroneCrate.SLOT_FLUID_ID,
                        125,
                        53,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineSlots = BlockEntityDroneCrate.SLOT_COUNT;
                    if (index < machineSlots)
                        return moveItemStackTo(stack, machineSlots, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem) {
                        return moveItemStackTo(
                                stack, BlockEntityDroneCrate.SLOT_FLUID_ID, machineSlots, false);
                    }
                    return moveItemStackTo(stack, 0, BlockEntityDroneCrate.CARGO_SLOTS, false);
                });
    }
}

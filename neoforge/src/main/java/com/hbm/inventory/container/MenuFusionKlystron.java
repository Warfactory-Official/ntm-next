// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystron;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuFusionKlystron extends BlockEntityMenu<BlockEntityFusionKlystron> {

    private static final int SLOT_COUNT = 1;

    public MenuFusionKlystron(int containerId, Inventory playerInv, BlockEntityFusionKlystron be) {
        super(ModMenus.FUSION_KLYSTRON.get(), containerId, be);
        checkContainerSize(be, SLOT_COUNT);

        addSlot(new SlotFiltered(be, 0, 8, 72, IBatteryItem::isBattery));

        addStandardInventorySlots(playerInv, 17, 118);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < SLOT_COUNT)
                        return moveItemStackTo(stack, SLOT_COUNT, slots.size(), true);
                    if (IBatteryItem.isBattery(stack)) return moveItemStackTo(stack, 0, 1, false);
                    return false;
                });
    }
}

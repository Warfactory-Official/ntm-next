// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.tileentity.machine.BlockEntityCoreInjector;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuCoreInjector extends BlockEntityMenu<BlockEntityCoreInjector> {

    public MenuCoreInjector(int containerId, Inventory playerInv, BlockEntityCoreInjector be) {
        super(ModMenus.CORE_INJECTOR.get(), containerId, be);
        checkContainerSize(be, BlockEntityCoreInjector.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityCoreInjector.SLOT_FILL_A,
                        26,
                        17,
                        s -> be.canPlaceItem(BlockEntityCoreInjector.SLOT_FILL_A, s)));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityCoreInjector.SLOT_DRAIN_A, 26, 53));
        addSlot(
                new SlotFiltered(
                        be,
                        BlockEntityCoreInjector.SLOT_FILL_B,
                        134,
                        17,
                        s -> be.canPlaceItem(BlockEntityCoreInjector.SLOT_FILL_B, s)));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BlockEntityCoreInjector.SLOT_DRAIN_B, 134, 53));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack ->
                        index < BlockEntityCoreInjector.SLOT_COUNT
                                && moveItemStackTo(
                                        stack,
                                        BlockEntityCoreInjector.SLOT_COUNT,
                                        slots.size(),
                                        true));
    }
}

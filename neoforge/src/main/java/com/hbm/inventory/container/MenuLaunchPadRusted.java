// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.ModItems;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadRusted;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuLaunchPadRusted extends BlockEntityMenu<BlockEntityLaunchPadRusted> {
    private static final int SLOTS = BlockEntityLaunchPadRusted.SLOT_COUNT;

    public MenuLaunchPadRusted(int id, Inventory playerInventory, BlockEntityLaunchPadRusted pad) {
        this(id, playerInventory, pad, pad);
    }

    private MenuLaunchPadRusted(
            int id,
            Inventory playerInventory,
            Container container,
            BlockEntityLaunchPadRusted pad) {
        super(ModMenus.LAUNCH_PAD_RUSTED.get(), id, pad, container);
        checkContainerSize(container, SLOTS);
        addSlot(new SlotRecipeOutput(playerInventory.player, container, 0, 26, 72));
        addSlot(
                new SlotFiltered(
                        container, 1, 116, 45, stack -> stack.is(ModItems.LAUNCH_CODE.get())));
        addSlot(
                new SlotFiltered(
                        container, 2, 134, 45, stack -> stack.is(ModItems.LAUNCH_KEY.get())));
        addSlot(
                new SlotFiltered(
                        container, 3, 26, 99, stack -> stack.getItem() instanceof IDesignatorItem));
        addStandardInventorySlots(playerInventory, 8, 154);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < SLOTS) return moveItemStackTo(stack, SLOTS, slots.size(), true);
                    if (stack.getItem() instanceof IDesignatorItem)
                        return moveItemStackTo(stack, 3, 4, false);
                    if (stack.is(ModItems.LAUNCH_CODE.get()))
                        return moveItemStackTo(stack, 1, 2, false);
                    if (stack.is(ModItems.LAUNCH_KEY.get()))
                        return moveItemStackTo(stack, 2, 3, false);
                    return false;
                });
    }
}

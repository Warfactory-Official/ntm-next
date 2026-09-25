// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.tileentity.bomb.BlockEntityNukeBalefire;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuNukeFstbmb extends BlockEntityMenu<BlockEntityNukeBalefire> {

    private static final int PLAYER_DROP = 56;

    public MenuNukeFstbmb(int containerId, Inventory playerInv, BlockEntityNukeBalefire bomb) {
        super(ModMenus.NUKE_FSTBMB.get(), containerId, bomb);
        checkContainerSize(bomb, BlockEntityNukeBalefire.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityNukeBalefire.SLOT_EGG,
                        17,
                        36,
                        stack -> stack.is(ModItems.EGG_BALEFIRE.get())));
        addSlot(
                new SlotFiltered(
                        container(),
                        BlockEntityNukeBalefire.SLOT_BATTERY,
                        53,
                        36,
                        stack ->
                                stack.is(ModItems.BATTERY_SPARK.get())
                                        || stack.is(ModItems.BATTERY_TRIXITE.get())));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                playerInv,
                                col + row * 9 + 9,
                                8 + col * 18,
                                84 + row * 18 + PLAYER_DROP));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142 + PLAYER_DROP));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int bomb = BlockEntityNukeBalefire.SLOT_COUNT;
                    if (index < bomb) return moveItemStackTo(stack, bomb, slots.size(), true);
                    return moveItemStackTo(stack, 0, bomb, false);
                });
    }
}

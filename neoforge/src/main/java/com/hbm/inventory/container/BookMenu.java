/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.inventory.container;

import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.items.ModItems;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

public final class BookMenu extends NtmContainerMenu {

    private static final int RESULT_SLOT = 0;
    private static final int INPUT_START = 1;
    private static final int INPUT_END = 5;
    private static final int INVENTORY_START = 5;
    private static final int INVENTORY_MAIN_END = 32;
    private static final int INVENTORY_END = 41;

    private final Player player;
    private final TransientCraftingContainer craftMatrix =
            new TransientCraftingContainer(this, 2, 2);
    private final Container craftResult = new SimpleContainer(1);

    public BookMenu(int containerId, Inventory inventory) {
        super(ModMenus.BOOK_OF.get(), containerId, null);
        this.player = inventory.player;

        addSlot(
                new Slot(craftResult, 0, 124, 35) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        stack.getItem().onCraftedBy(stack, player);
                        for (int slot = 0; slot < craftMatrix.getContainerSize(); slot++) {
                            if (!craftMatrix.getItem(slot).isEmpty())
                                craftMatrix.removeItem(slot, 1);
                        }
                    }
                });

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                addSlot(new Slot(craftMatrix, column + row * 2, 30 + column * 36, 17 + row * 36));
            }
        }

        addStandardInventorySlots(inventory, 8, 84);
        slotsChanged(craftMatrix);
    }

    @Override
    public void slotsChanged(Container container) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack result = MagicRecipes.INSTANCE.getRecipe(craftMatrix);
        craftResult.setItem(RESULT_SLOT, result);
        setRemoteSlot(RESULT_SLOT, result);
        serverPlayer.connection.send(
                new ClientboundContainerSetSlotPacket(
                        containerId, incrementStateId(), RESULT_SLOT, result));
    }

    public boolean hasResult() {
        return !craftResult.getItem(RESULT_SLOT).isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.BOOK_OF.get()));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player instanceof ServerPlayer)) return;

        for (int slot = 0; slot < craftMatrix.getContainerSize(); slot++) {
            ItemStack stack = craftMatrix.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index == RESULT_SLOT)
                        return moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true);
                    if (index >= INVENTORY_START && index < INVENTORY_MAIN_END)
                        return moveItemStackTo(stack, INVENTORY_MAIN_END, INVENTORY_END, false);
                    if (index >= INVENTORY_MAIN_END && index < INVENTORY_END)
                        return moveItemStackTo(stack, INVENTORY_START, INVENTORY_MAIN_END, false);
                    return moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false) || false;
                });
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot target) {
        return target.container != craftResult && super.canTakeItemForPickAll(stack, target);
    }
}

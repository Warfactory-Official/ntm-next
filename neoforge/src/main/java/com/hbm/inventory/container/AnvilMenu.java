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

import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipes;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class AnvilMenu extends NtmContainerMenu {

    private static final int INPUT_COUNT = 2;
    private static final int RESULT_SLOT = INPUT_COUNT;
    private static final int PLAYER_START = RESULT_SLOT + 1;

    public final int tier;

    private final Inventory playerInventory;
    private final SimpleContainer inputs =
            new SimpleContainer(INPUT_COUNT) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    if (!consumingInputs) {
                        refreshPreview();
                    }
                }
            };
    private final SimpleContainer result = new SimpleContainer(1);
    private @Nullable AnvilSmithingRecipe selectedRecipe;
    private boolean mirrored;
    private boolean consumingInputs;
    private long expiration = Long.MAX_VALUE;

    public AnvilMenu(int containerId, Inventory playerInventory, int tier) {
        super(ModMenus.anvilMenuType(tier).get(), containerId, null);
        this.playerInventory = playerInventory;
        this.tier = tier;

        addSlot(new Slot(inputs, 0, 17, 27));
        addSlot(new Slot(inputs, 1, 53, 27));
        addSlot(
                new SlotRecipeOutput(playerInventory.player, result, 0, 89, 27) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return prepareResultTake();
                    }

                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        super.onTake(player, stack);
                        consumeSelectedRecipe();
                    }
                });
        addStandardInventorySlots(playerInventory, 8, 140);
    }

    private void refreshPreview() {
        selectedRecipe = null;
        mirrored = false;
        expiration = Long.MAX_VALUE;

        var left = inputs.getItem(0);
        var right = inputs.getItem(1);
        long gameTime = playerInventory.player.level().getGameTime();
        for (var recipe : AnvilSmithingRecipes.INSTANCE.recipes()) {
            if (recipe.tier > tier) {
                continue;
            }
            int match = recipe.matchesInt(left, right, gameTime);
            if (match >= 0) {
                selectedRecipe = recipe;
                mirrored = match == 1;
                expiration = recipe.expiresAt(left, right);
                result.setItem(0, recipe.getOutput(left, right, gameTime));
                return;
            }
        }
        result.setItem(0, ItemStack.EMPTY);
    }

    private boolean prepareResultTake() {
        var recipe = selectedRecipe;
        if (recipe == null) {
            return false;
        }

        var left = inputs.getItem(0);
        var right = inputs.getItem(1);
        long gameTime = playerInventory.player.level().getGameTime();
        if (AnvilSmithingRecipes.INSTANCE.getRecipe(recipe.getInternalName()) != recipe
                || recipe.tier > tier) {
            refreshPreview();
            return false;
        }
        int match = recipe.matchesInt(left, right, gameTime);
        if (match < 0) {
            refreshPreview();
            return false;
        }

        mirrored = match == 1;
        expiration = recipe.expiresAt(left, right);
        var output = result.getItem(0);
        if (recipe.kind == AnvilSmithingRecipe.Kind.HOT && !output.isEmpty()) {
            recipe.updateOutputHeat(output, left, right, gameTime);
        }
        return !output.isEmpty();
    }

    private void consumeSelectedRecipe() {
        var recipe = selectedRecipe;
        consumingInputs = true;
        try {
            inputs.removeItem(0, recipe.amountConsumed(0, mirrored));
            inputs.removeItem(1, recipe.amountConsumed(1, mirrored));
        } finally {
            consumingInputs = false;
        }
        refreshPreview();
    }

    @Override
    public void broadcastChanges() {
        if (selectedRecipe != null && playerInventory.player.level().getGameTime() >= expiration) {
            refreshPreview();
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (player.isSpectator()) {
            return ItemStack.EMPTY;
        }

        var slot = getSlot(slotIndex);
        if (slot.getItem().isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (slotIndex == RESULT_SLOT && !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        var source = slot.getItem();
        var original = source.copy();
        boolean moved;
        if (slotIndex < PLAYER_START) {
            moved = moveItemStackTo(source, PLAYER_START, slots.size(), true);
        } else {
            moved = moveItemStackTo(source, 0, INPUT_COUNT, false);
        }
        if (!moved || source.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        if (slotIndex == RESULT_SLOT) {
            slot.onQuickCraft(source, original);
        }
        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, source);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level() instanceof ServerLevel) {
            consumingInputs = true;
            try {
                clearContainer(player, inputs);
            } finally {
                consumingInputs = false;
            }
            result.clearContent();
        }
    }
}

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

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.crafting.HbmCustomRecipe;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.RBMKPelletItem;
import com.hbm.items.machine.RBMKRodItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RBMKFuelDisassemblyRecipe extends HbmCustomRecipe {

    public static final RBMKFuelDisassemblyRecipe INSTANCE = new RBMKFuelDisassemblyRecipe();
    public static final MapCodec<RBMKFuelDisassemblyRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, RBMKFuelDisassemblyRecipe>
            STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<RBMKFuelDisassemblyRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static ItemStack getRod(CraftingInput input) {
        if (input.ingredientCount() != 1) return ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean matchesInput(CraftingInput input, Level level) {
        ItemStack stack = getRod(input);
        return stack.getItem() instanceof RBMKRodItem rod
                && rod.pellet != null
                && RBMKRodItem.getHullHeat(stack) < 50
                && RBMKRodItem.getCoreHeat(stack) < 50;
    }

    @Override
    public boolean acceptsInputState(CraftingInput input, int slot, ItemStack output) {
        return RecipeInputState.only(input.getItem(slot), ModDataComponents.RBMK_FUEL.get());
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack stack = getRod(input);

        if (stack.getItem() instanceof RBMKRodItem rod) {

            if (rod.pellet == null) return ItemStack.EMPTY;

            if (RBMKRodItem.getEnrichment(stack) > 0.99D) return ItemStack.EMPTY;

            if (RBMKRodItem.getHullHeat(stack) < 50 && RBMKRodItem.getCoreHeat(stack) < 50) {
                int enrichment =
                        4
                                - Mth.clamp(
                                        (int) Math.ceil(RBMKRodItem.getEnrichment(stack) * 5 - 1),
                                        0,
                                        4);
                ItemStack result = new ItemStack(rod.pellet, 8);
                result.set(
                        ModDataComponents.RBMK_PELLET.get(),
                        new RBMKPelletItem.Stage(
                                enrichment, RBMKRodItem.getPoisonLevel(stack) >= 0.5D));
                return result;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<RBMKFuelDisassemblyRecipe> getSerializer() {
        return SERIALIZER;
    }
}

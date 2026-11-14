/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ScrapsItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class ScrapsSplittingRecipe extends HbmCustomRecipe {

    public static final ScrapsSplittingRecipe INSTANCE = new ScrapsSplittingRecipe();
    public static final MapCodec<ScrapsSplittingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ScrapsSplittingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ScrapsSplittingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static @Nullable MaterialStack contents(CraftingInput input) {
        if (input.ingredientCount() != 1) return null;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            MaterialStack contents = ScrapsItem.getMats(stack);
            return contents != null && contents.amount >= 2 ? contents : null;
        }
        return null;
    }

    @Override
    protected boolean matchesInput(CraftingInput input, Level level) {
        return contents(input) != null;
    }

    @Override
    public boolean acceptsInputState(CraftingInput input, int slot, ItemStack output) {
        return RecipeInputState.only(input.getItem(slot), ModDataComponents.SCRAP.get());
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        MaterialStack contents = contents(input);
        if (contents == null) return ItemStack.EMPTY;
        boolean liquid =
                input.items().stream()
                        .filter(stack -> !stack.isEmpty())
                        .findFirst()
                        .map(ScrapsItem::isLiquid)
                        .orElseThrow();
        ItemStack result =
                ScrapsItem.create(
                        new MaterialStack(contents.material, contents.amount / 2), liquid);
        result.setCount(2);
        return result;
    }

    @Override
    public RecipeSerializer<ScrapsSplittingRecipe> getSerializer() {
        return SERIALIZER;
    }
}

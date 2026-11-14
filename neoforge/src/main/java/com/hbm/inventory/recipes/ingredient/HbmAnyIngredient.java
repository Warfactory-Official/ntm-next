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

package com.hbm.inventory.recipes.ingredient;

import com.hbm.NuclearTech;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public record HbmAnyIngredient(List<Ingredient> ingredients)
        implements IngredientState, ICustomIngredient {
    public static final Identifier ID = RegistryUtil.id("any");
    private static final MapCodec<HbmAnyIngredient> CODEC =
            Ingredient.CODEC
                    .listOf(1, Integer.MAX_VALUE)
                    .fieldOf("ingredients")
                    .xmap(HbmAnyIngredient::new, HbmAnyIngredient::ingredients);
    private static final StreamCodec<RegistryFriendlyByteBuf, HbmAnyIngredient> STREAM_CODEC =
            Ingredient.CONTENTS_STREAM_CODEC
                    .apply(ByteBufCodecs.list())
                    .map(HbmAnyIngredient::new, HbmAnyIngredient::ingredients);

    public HbmAnyIngredient {
        ingredients = List.copyOf(ingredients);
        assert !ingredients.isEmpty();
    }

    @Override
    public boolean test(ItemStack stack) {
        for (Ingredient ingredient : ingredients) if (ingredient.test(stack)) return true;
        return false;
    }

    @Override
    public boolean acceptsState(ItemStack stack) {
        return acceptsState(stack, DataComponentMatchers.ANY);
    }

    @Override
    public boolean acceptsState(ItemStack stack, DataComponentMatchers additional) {
        for (Ingredient ingredient : ingredients) {
            if (ingredient.test(stack) && RecipeInputState.accepts(ingredient, stack, additional))
                return true;
        }
        return false;
    }

    @Override
    public Stream<Holder<Item>> items() {
        return ingredients.stream().flatMap(Ingredient::items);
    }

    @Override
    public SlotDisplay display() {
        return new SlotDisplay.Composite(ingredients.stream().map(Ingredient::display).toList());
    }

    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, NuclearTech.MOD_ID);
    public static final DeferredHolder<IngredientType<?>, IngredientType<HbmAnyIngredient>> TYPE =
            INGREDIENT_TYPES.register(
                    ID.getPath(), () -> new IngredientType<>(CODEC, STREAM_CODEC));

    public static void subscribe(IEventBus modBus) {
        INGREDIENT_TYPES.register(modBus);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }
}

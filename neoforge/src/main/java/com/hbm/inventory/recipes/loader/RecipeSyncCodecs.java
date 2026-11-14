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

package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.ModRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class RecipeSyncCodecs {
    private static final StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> RECIPE =
            Identifier.STREAM_CODEC
                    .<RegistryFriendlyByteBuf>cast()
                    .map(
                            BuiltInRegistries.RECIPE_SERIALIZER::getValue,
                            BuiltInRegistries.RECIPE_SERIALIZER::getKey)
                    .dispatch(Recipe::getSerializer, RecipeSerializer::streamCodec);
    private static final StreamCodec<RegistryFriendlyByteBuf, RecipeHolder<?>> HOLDER =
            StreamCodec.composite(
                    ResourceKey.streamCodec(Registries.RECIPE),
                    RecipeHolder::id,
                    RECIPE,
                    RecipeHolder::value,
                    RecipeHolder::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, List<RecipeHolder<?>>> RECIPES =
            HOLDER.apply(ByteBufCodecs.list());

    private RecipeSyncCodecs() {}

    public static List<RecipeHolder<?>> snapshot(MinecraftServer server) {
        Set<RecipeType<?>> tables = Set.copyOf(ModRecipes.datapackBackedTypes());
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (tables.contains(holder.value().getType())
                    || ArcFurnaceRecipes.yieldsFurnaceRows(holder)) {
                recipes.add(holder);
            }
        }
        return List.copyOf(recipes);
    }
}

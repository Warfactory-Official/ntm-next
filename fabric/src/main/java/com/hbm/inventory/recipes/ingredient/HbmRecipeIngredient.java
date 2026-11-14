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

import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
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

public record HbmRecipeIngredient(Ingredient base, boolean remainder)
        implements IngredientState, CustomIngredient {
    public static final Identifier ID = RegistryUtil.id("recipe_input");

    private static final MapCodec<HbmRecipeIngredient> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Ingredient.CODEC
                                                    .fieldOf("base")
                                                    .forGetter(HbmRecipeIngredient::base),
                                            Codec.BOOL
                                                    .optionalFieldOf("remainder", false)
                                                    .forGetter(HbmRecipeIngredient::remainder))
                                    .apply(i, HbmRecipeIngredient::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, HbmRecipeIngredient> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    HbmRecipeIngredient::base,
                    ByteBufCodecs.BOOL,
                    HbmRecipeIngredient::remainder,
                    HbmRecipeIngredient::new);

    @Override
    public boolean test(ItemStack stack) {
        return base.test(stack)
                && (RecipeInputState.accepts(base, stack)
                        || remainder && RecipeInputState.preservesRemainder(stack));
    }

    @Override
    public boolean acceptsState(ItemStack stack) {
        return RecipeInputState.accepts(base, stack);
    }

    @Override
    public boolean acceptsState(ItemStack stack, DataComponentMatchers additional) {
        return RecipeInputState.accepts(base, stack, additional);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return base.items();
    }

    @Override
    public SlotDisplay display() {
        return base.display();
    }

    public static final CustomIngredientSerializer<HbmRecipeIngredient> SERIALIZER =
            new Serializer();

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private static final class Serializer
            implements CustomIngredientSerializer<HbmRecipeIngredient> {
        @Override
        public Identifier getIdentifier() {
            return ID;
        }

        @Override
        public MapCodec<HbmRecipeIngredient> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HbmRecipeIngredient> getStreamCodec() {
            return STREAM_CODEC;
        }
    }
}

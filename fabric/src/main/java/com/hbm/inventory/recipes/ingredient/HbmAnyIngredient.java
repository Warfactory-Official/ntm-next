// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.lib.Library;
import com.mojang.serialization.MapCodec;
import java.util.List;
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

public record HbmAnyIngredient(List<Ingredient> ingredients)
        implements IngredientState, CustomIngredient {
    public static final Identifier ID = Library.id("any");
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

    public static final CustomIngredientSerializer<HbmAnyIngredient> SERIALIZER = new Serializer();

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private static final class Serializer implements CustomIngredientSerializer<HbmAnyIngredient> {
        @Override
        public Identifier getIdentifier() {
            return ID;
        }

        @Override
        public MapCodec<HbmAnyIngredient> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HbmAnyIngredient> getStreamCodec() {
            return STREAM_CODEC;
        }
    }
}

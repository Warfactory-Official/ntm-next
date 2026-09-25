// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class BreederRecipe extends GenericRecipe {

    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(Codec.INT.fieldOf("flux").forGetter(Extras::flux))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.of(
                    (buf, extras) -> buf.writeVarInt(extras.flux()),
                    buf -> new Extras(buf.readVarInt()));
    private static final BiConsumer<BreederRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> recipe.flux = extras.flux();
    private static final Function<BreederRecipe, Extras> EXTRACT_EXTRAS =
            recipe -> new Extras(recipe.flux);

    public static final MapCodec<BreederRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    BreederRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, BreederRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    BreederRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<BreederRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public int flux;

    public BreederRecipe(String name) {
        super(name);
    }

    public BreederRecipe setFlux(int flux) {
        this.flux = flux;
        return this;
    }

    public CountIngredient input() {
        return inputItem[0];
    }

    public ItemStack output() {
        return outputItems() == null || outputItems().length == 0
                ? ItemStack.EMPTY
                : outputItems()[0].unwrap().getFirst().value();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return BreederRecipes.INSTANCE;
    }

    private record Extras(int flux) {}
}

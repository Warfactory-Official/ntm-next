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

public class CyclotronRecipe extends GenericRecipe {

    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(Codec.INT.fieldOf("amat").forGetter(Extras::amat))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.of(
                    (buf, extras) -> buf.writeVarInt(extras.amat()),
                    buf -> new Extras(buf.readVarInt()));
    private static final BiConsumer<CyclotronRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> recipe.amat = extras.amat();
    private static final Function<CyclotronRecipe, Extras> EXTRACT_EXTRAS =
            recipe -> new Extras(recipe.amat);

    public static final MapCodec<CyclotronRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    CyclotronRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, CyclotronRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    CyclotronRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<CyclotronRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public int amat;

    public CyclotronRecipe(String name) {
        super(name);
    }

    public CyclotronRecipe setAmat(int amat) {
        this.amat = amat;
        return this;
    }

    public CountIngredient particle() {
        return inputItem[0];
    }

    public CountIngredient target() {
        return inputItem[1];
    }

    public ItemStack output() {
        return outputItems() == null || outputItems().length == 0
                ? ItemStack.EMPTY
                : outputItems()[0].unwrap().getFirst().value();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return CyclotronRecipes.INSTANCE;
    }

    private record Extras(int amat) {}
}

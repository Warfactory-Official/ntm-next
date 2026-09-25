// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemStamp.StampType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PressRecipe extends GenericRecipe {

    private static final Codec<StampType> STAMP_TYPE_CODEC =
            Codec.STRING.xmap(
                    name -> StampType.valueOf(name.toUpperCase(Locale.ROOT)),
                    type -> type.name().toLowerCase(Locale.ROOT));
    private static final MapCodec<StampType> STAMP_MAP_CODEC = STAMP_TYPE_CODEC.fieldOf("stamp");
    private static final StreamCodec<RegistryFriendlyByteBuf, StampType> STAMP_STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> StampType.values()[id], Enum::ordinal).cast();
    private static final BiConsumer<PressRecipe, StampType> APPLY_STAMP =
            (recipe, stampType) -> recipe.stampType = stampType;
    private static final Function<PressRecipe, StampType> EXTRACT_STAMP =
            recipe -> recipe.stampType;
    public static final MapCodec<PressRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(PressRecipe::new, STAMP_MAP_CODEC, APPLY_STAMP, EXTRACT_STAMP);
    public static final StreamCodec<RegistryFriendlyByteBuf, PressRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    PressRecipe::new, STAMP_STREAM_CODEC, APPLY_STAMP, EXTRACT_STAMP);
    public static final RecipeSerializer<PressRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public StampType stampType;

    public PressRecipe(String name) {
        super(name);
    }

    public PressRecipe setStampType(StampType stampType) {
        this.stampType = stampType;
        return this;
    }

    public StampType type() {
        return stampType;
    }

    public CountIngredient input() {
        return inputItem[0];
    }

    public ItemStack output() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return PressRecipes.INSTANCE;
    }
}

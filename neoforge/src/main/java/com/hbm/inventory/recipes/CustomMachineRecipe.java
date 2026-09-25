// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class CustomMachineRecipe extends GenericRecipe {

    private static final Codec<PollutionType> POLLUTION_TYPE =
            Codec.stringResolver(
                    Enum::name,
                    name -> {
                        for (PollutionType type : PollutionType.VALUES)
                            if (type.name().equals(name)) return type;
                        return null;
                    });
    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.STRING
                                                    .fieldOf("recipe_key")
                                                    .forGetter(Extras::recipeKey),
                                            POLLUTION_TYPE
                                                    .optionalFieldOf("pollution_type")
                                                    .forGetter(Extras::pollutionType),
                                            Codec.FLOAT
                                                    .optionalFieldOf("pollution_amount", 0F)
                                                    .forGetter(Extras::pollutionAmount),
                                            Codec.FLOAT
                                                    .optionalFieldOf("radiation_amount", 0F)
                                                    .forGetter(Extras::radiationAmount),
                                            Codec.INT
                                                    .optionalFieldOf("flux", 0)
                                                    .forGetter(Extras::flux),
                                            Codec.INT
                                                    .optionalFieldOf("heat", 0)
                                                    .forGetter(Extras::heat))
                                    .apply(i, Extras::new));
    private static final StreamCodec<ByteBuf, Optional<PollutionType>> POLLUTION_TYPE_STREAM =
            ByteBufCodecs.VAR_INT.<Optional<PollutionType>>map(
                    id -> id == 0 ? Optional.empty() : Optional.of(PollutionType.VALUES[id - 1]),
                    type -> type.<Integer>map(t -> t.ordinal() + 1).orElse(0));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    Extras::recipeKey,
                    POLLUTION_TYPE_STREAM,
                    Extras::pollutionType,
                    ByteBufCodecs.FLOAT,
                    Extras::pollutionAmount,
                    ByteBufCodecs.FLOAT,
                    Extras::radiationAmount,
                    ByteBufCodecs.VAR_INT,
                    Extras::flux,
                    ByteBufCodecs.VAR_INT,
                    Extras::heat,
                    Extras::new);
    private static final BiConsumer<CustomMachineRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.recipeKey = extras.recipeKey();
                recipe.pollutionType = extras.pollutionType().orElse(null);
                recipe.pollutionAmount = extras.pollutionAmount();
                recipe.radiationAmount = extras.radiationAmount();
                recipe.flux = extras.flux();
                recipe.heat = extras.heat();
            };
    private static final Function<CustomMachineRecipe, Extras> EXTRACT_EXTRAS =
            recipe ->
                    new Extras(
                            recipe.recipeKey,
                            Optional.ofNullable(recipe.pollutionType),
                            recipe.pollutionAmount,
                            recipe.radiationAmount,
                            recipe.flux,
                            recipe.heat);
    public static final MapCodec<CustomMachineRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    CustomMachineRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, CustomMachineRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    CustomMachineRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<CustomMachineRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public String recipeKey = "";
    public @Nullable PollutionType pollutionType;
    public float pollutionAmount;
    public float radiationAmount;
    public int flux;
    public int heat;

    public CustomMachineRecipe(String name) {
        super(name);
    }

    public CustomMachineRecipe setRecipeKey(String recipeKey) {
        this.recipeKey = recipeKey;
        return this;
    }

    public CustomMachineRecipe setPollution(PollutionType type, float amount) {
        this.pollutionType = type;
        this.pollutionAmount = amount;
        return this;
    }

    public CustomMachineRecipe setRadiation(float amount) {
        this.radiationAmount = amount;
        return this;
    }

    public CustomMachineRecipe setFlux(int flux) {
        this.flux = flux;
        return this;
    }

    public CustomMachineRecipe setHeat(int heat) {
        this.heat = heat;
        return this;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return CustomMachineRecipes.INSTANCE;
    }

    private record Extras(
            String recipeKey,
            Optional<PollutionType> pollutionType,
            float pollutionAmount,
            float radiationAmount,
            int flux,
            int heat) {}
}

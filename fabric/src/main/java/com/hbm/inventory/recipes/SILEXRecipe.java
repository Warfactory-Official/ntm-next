// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.EnumWavelengths;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SILEXRecipe extends GenericRecipe {

    private static final Codec<EnumWavelengths> WAVELENGTH_CODEC =
            Codec.STRING.xmap(
                    name -> EnumWavelengths.valueOf(name.toUpperCase(Locale.ROOT)),
                    wavelength -> wavelength.name().toLowerCase(Locale.ROOT));
    private static final StreamCodec<ByteBuf, EnumWavelengths> WAVELENGTH_STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> EnumWavelengths.values()[id], Enum::ordinal);
    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .fieldOf("fluid_produced")
                                                    .forGetter(Extras::fluidProduced),
                                            Codec.INT
                                                    .fieldOf("fluid_consumed")
                                                    .forGetter(Extras::fluidConsumed),
                                            WAVELENGTH_CODEC
                                                    .fieldOf("laser_strength")
                                                    .forGetter(Extras::laserStrength))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    Extras::fluidProduced,
                    ByteBufCodecs.VAR_INT,
                    Extras::fluidConsumed,
                    WAVELENGTH_STREAM_CODEC,
                    Extras::laserStrength,
                    Extras::new);
    private static final BiConsumer<SILEXRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.fluidProduced = extras.fluidProduced();
                recipe.fluidConsumed = extras.fluidConsumed();
                recipe.laserStrength = extras.laserStrength();
            };
    private static final Function<SILEXRecipe, Extras> EXTRACT_EXTRAS =
            recipe -> new Extras(recipe.fluidProduced, recipe.fluidConsumed, recipe.laserStrength);
    public static final MapCodec<SILEXRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    SILEXRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, SILEXRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    SILEXRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<SILEXRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public int fluidProduced;
    public int fluidConsumed;
    public EnumWavelengths laserStrength;

    public SILEXRecipe(String name) {
        super(name);
    }

    public SILEXRecipe setup(int fluidProduced, int fluidConsumed, int laserStrength) {
        return setup(fluidProduced, fluidConsumed, EnumWavelengths.values()[laserStrength]);
    }

    public SILEXRecipe setup(int fluidProduced, int fluidConsumed, EnumWavelengths laserStrength) {
        this.fluidProduced = fluidProduced;
        this.fluidConsumed = fluidConsumed;
        this.laserStrength = laserStrength;
        return this;
    }

    public WeightedList<ItemStack> outputs() {
        return outputItems().length == 0 ? WeightedList.of() : outputItems()[0];
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return SILEXRecipes.INSTANCE;
    }

    private record Extras(int fluidProduced, int fluidConsumed, EnumWavelengths laserStrength) {}
}

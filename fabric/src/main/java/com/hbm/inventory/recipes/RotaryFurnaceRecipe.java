// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class RotaryFurnaceRecipe extends GenericRecipe {

    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            MaterialStack.CODEC
                                                    .fieldOf("output_material")
                                                    .forGetter(Extras::output),
                                            Codec.INT
                                                    .optionalFieldOf("steam", 0)
                                                    .forGetter(Extras::steam))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.of(
                    (buf, extras) -> {
                        MaterialStack.STREAM_CODEC.encode(buf, extras.output());
                        buf.writeVarInt(extras.steam());
                    },
                    buf -> new Extras(MaterialStack.STREAM_CODEC.decode(buf), buf.readVarInt()));
    private static final BiConsumer<RotaryFurnaceRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.output = extras.output();
                recipe.steam = extras.steam();
            };
    private static final Function<RotaryFurnaceRecipe, Extras> EXTRACT_EXTRAS =
            recipe -> new Extras(recipe.output, recipe.steam);
    public static final MapCodec<RotaryFurnaceRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    RotaryFurnaceRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, RotaryFurnaceRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    RotaryFurnaceRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<RotaryFurnaceRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public MaterialStack output;

    public int steam;

    public RotaryFurnaceRecipe(String name) {
        super(name);
    }

    public RotaryFurnaceRecipe setOutput(MaterialStack output) {
        this.output = output;
        return this;
    }

    public RotaryFurnaceRecipe setSteam(int steam) {
        this.steam = steam;
        return this;
    }

    public @Nullable FluidStackNTM fluid() {
        return inputFluid.length > 0 ? inputFluid[0] : null;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return RotaryFurnaceRecipes.INSTANCE;
    }

    private record Extras(MaterialStack output, int steam) {}
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class ElectrolyserMetalRecipe extends GenericRecipe {

    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            MaterialStack.CODEC
                                                    .fieldOf("output_material")
                                                    .forGetter(Extras::output1),
                                            MaterialStack.CODEC
                                                    .optionalFieldOf("output_material_2")
                                                    .forGetter(Extras::output2))
                                    .apply(i, Extras::new));
    private static final StreamCodec<ByteBuf, Optional<MaterialStack>>
            OPTIONAL_MATERIAL_STREAM_CODEC = ByteBufCodecs.optional(MaterialStack.STREAM_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.composite(
                    MaterialStack.STREAM_CODEC,
                    Extras::output1,
                    OPTIONAL_MATERIAL_STREAM_CODEC,
                    Extras::output2,
                    Extras::new);
    private static final BiConsumer<ElectrolyserMetalRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.output1 = extras.output1();
                recipe.output2 = extras.output2().orElse(null);
            };
    private static final Function<ElectrolyserMetalRecipe, Extras> EXTRACT_EXTRAS =
            recipe -> new Extras(recipe.output1, Optional.ofNullable(recipe.output2));
    public static final MapCodec<ElectrolyserMetalRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    ElectrolyserMetalRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, ElectrolyserMetalRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    ElectrolyserMetalRecipe::new,
                    EXTRAS_STREAM_CODEC,
                    APPLY_EXTRAS,
                    EXTRACT_EXTRAS);
    public static final RecipeSerializer<ElectrolyserMetalRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public MaterialStack output1;
    public @Nullable MaterialStack output2;

    public ElectrolyserMetalRecipe(String name) {
        super(name);
    }

    public ElectrolyserMetalRecipe outputMaterials(
            MaterialStack output1, @Nullable MaterialStack output2) {
        this.output1 = output1;
        this.output2 = output2;
        return this;
    }

    public int byproductCount() {
        return outputItems() == null ? 0 : outputItems().length;
    }

    public ItemStack byproduct(int index) {
        return outputItems()[index].unwrap().get(0).value();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return ElectrolyserMetalRecipes.INSTANCE;
    }

    private record Extras(MaterialStack output1, Optional<MaterialStack> output2) {}
}

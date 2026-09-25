// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class ArcFurnaceRecipe extends GenericRecipe {

    private static final MapCodec<List<MaterialStack>> EXTRAS_MAP_CODEC =
            MaterialStack.CODEC.listOf().optionalFieldOf("output_materials", List.of());
    private static final StreamCodec<ByteBuf, List<MaterialStack>> MATERIALS_STREAM_CODEC =
            MaterialStack.STREAM_CODEC.apply(ByteBufCodecs.list());

    private static final StreamCodec<RegistryFriendlyByteBuf, List<MaterialStack>>
            EXTRAS_STREAM_CODEC =
                    StreamCodec.of(
                            (buf, materials) -> MATERIALS_STREAM_CODEC.encode(buf, materials),
                            MATERIALS_STREAM_CODEC::decode);

    private static final BiConsumer<ArcFurnaceRecipe, List<MaterialStack>> APPLY_EXTRAS =
            (recipe, materials) ->
                    recipe.fluidOutput =
                            materials.isEmpty() ? null : materials.toArray(MaterialStack[]::new);
    private static final Function<ArcFurnaceRecipe, List<MaterialStack>> EXTRACT_EXTRAS =
            recipe -> recipe.fluidOutput == null ? List.of() : Arrays.asList(recipe.fluidOutput);
    public static final MapCodec<ArcFurnaceRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    ArcFurnaceRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcFurnaceRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    ArcFurnaceRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<ArcFurnaceRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    private MaterialStack @Nullable [] fluidOutput;

    public ArcFurnaceRecipe(String name) {
        super(name);
    }

    public ArcFurnaceRecipe outputMaterials(List<MaterialStack> materials) {
        this.fluidOutput = materials.isEmpty() ? null : materials.toArray(MaterialStack[]::new);
        return this;
    }

    public ArcFurnaceRecipe outputMaterials(MaterialStack... materials) {
        return outputMaterials(Arrays.asList(materials));
    }

    ArcFurnaceRecipe withId(ResourceKey<Recipe<?>> id) {
        this.id = id;
        return this;
    }

    public @Nullable ItemStack solidOutput() {
        if (outputItems() == null || outputItems().length == 0) return null;
        List<Weighted<ItemStack>> entries = outputItems()[0].unwrap();
        if (entries.isEmpty()) return null;
        ItemStack stack = entries.get(0).value();
        return stack.isEmpty() ? null : stack;
    }

    public MaterialStack @Nullable [] fluidOutput() {
        return fluidOutput;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return ArcFurnaceRecipes.INSTANCE;
    }
}

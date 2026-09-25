// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class CrystallizerRecipe extends GenericRecipe {

    private static final MapCodec<Float> PRODUCTIVITY_MAP_CODEC =
            Codec.FLOAT.optionalFieldOf("productivity", 0F);
    private static final StreamCodec<RegistryFriendlyByteBuf, Float> PRODUCTIVITY_STREAM_CODEC =
            ByteBufCodecs.FLOAT.cast();
    private static final BiConsumer<CrystallizerRecipe, Float> APPLY_PRODUCTIVITY =
            (recipe, productivity) -> recipe.productivity = productivity;
    private static final Function<CrystallizerRecipe, Float> EXTRACT_PRODUCTIVITY =
            recipe -> recipe.productivity;
    public static final MapCodec<CrystallizerRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    CrystallizerRecipe::new,
                    PRODUCTIVITY_MAP_CODEC,
                    APPLY_PRODUCTIVITY,
                    EXTRACT_PRODUCTIVITY);
    public static final StreamCodec<RegistryFriendlyByteBuf, CrystallizerRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    CrystallizerRecipe::new,
                    PRODUCTIVITY_STREAM_CODEC,
                    APPLY_PRODUCTIVITY,
                    EXTRACT_PRODUCTIVITY);
    public static final RecipeSerializer<CrystallizerRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public float productivity;

    public CrystallizerRecipe(String name) {
        super(name);
    }

    public CrystallizerRecipe setProductivity(float productivity) {
        this.productivity = productivity;
        return this;
    }

    public @Nullable Fluid acidType() {
        return inputFluid.length == 0 ? null : inputFluid[0].type();
    }

    public int acidAmount() {
        return (int) inputFluid[0].amount();
    }

    public int itemAmount() {
        return inputItem[0].count();
    }

    public ItemStack output() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return CrystallizerRecipes.INSTANCE;
    }
}

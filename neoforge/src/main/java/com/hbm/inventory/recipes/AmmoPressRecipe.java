// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AmmoPressRecipe extends GenericRecipe {

    public static final int GRID_SIZE = 9;

    private static final int[] EMPTY_CELLS = new int[0];
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Integer>> CELLS_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).cast();
    private static final BiConsumer<AmmoPressRecipe, List<Integer>> APPLY_CELLS =
            (recipe, cells) -> {
                recipe.cells = cells.stream().mapToInt(Integer::intValue).toArray();
                recipe.grid = null;
            };
    private static final Function<AmmoPressRecipe, List<Integer>> EXTRACT_CELLS =
            recipe -> Arrays.stream(recipe.cells).boxed().toList();
    public static final MapCodec<AmmoPressRecipe> MAP_CODEC =
            GenericRecipe.gridCodec(
                    AmmoPressRecipe::new,
                    (recipe, cells) -> recipe.setCells(cells),
                    recipe -> recipe.cells);
    public static final StreamCodec<RegistryFriendlyByteBuf, AmmoPressRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    AmmoPressRecipe::new, CELLS_STREAM_CODEC, APPLY_CELLS, EXTRACT_CELLS);
    public static final RecipeSerializer<AmmoPressRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public int[] cells = EMPTY_CELLS;

    private CountIngredient[] grid;

    public AmmoPressRecipe(String name) {
        super(name);
    }

    public AmmoPressRecipe setCells(int... cells) {
        this.cells = cells;
        this.grid = null;
        return this;
    }

    public CountIngredient[] input() {
        if (grid == null) {
            assert cells.length == inputItem.length;
            CountIngredient[] built = new CountIngredient[GRID_SIZE];
            for (int i = 0; i < cells.length; i++) built[cells[i]] = inputItem[i];
            grid = built;
        }
        return grid;
    }

    public ItemStack output() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return AmmoPressRecipes.INSTANCE;
    }
}

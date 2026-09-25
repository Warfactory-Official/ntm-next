// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.PedestalRecipes.ExtraCondition;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PedestalRecipe extends GenericRecipe {

    public static final int GRID_SIZE = 9;

    private static final int[] EMPTY_CELLS = new int[0];
    private static final Codec<ExtraCondition> EXTRA_CODEC =
            Codec.STRING.xmap(
                    name -> ExtraCondition.valueOf(name.toUpperCase(Locale.ROOT)),
                    condition -> condition.name().toLowerCase(Locale.ROOT));
    private static final StreamCodec<ByteBuf, ExtraCondition> EXTRA_STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> ExtraCondition.values()[id], Enum::ordinal);
    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            EXTRA_CODEC
                                                    .optionalFieldOf("extra", ExtraCondition.NONE)
                                                    .forGetter(Extras::extra),
                                            Codec.INT
                                                    .optionalFieldOf("recipe_set", 0)
                                                    .forGetter(Extras::recipeSet))
                                    .apply(i, (extra, set) -> new Extras(List.of(), extra, set)));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()),
                    Extras::cells,
                    EXTRA_STREAM_CODEC,
                    Extras::extra,
                    ByteBufCodecs.VAR_INT,
                    Extras::recipeSet,
                    Extras::new);
    private static final BiConsumer<PedestalRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.cells = extras.cells().stream().mapToInt(Integer::intValue).toArray();
                recipe.grid = null;
                recipe.extra = extras.extra();
                recipe.recipeSet = extras.recipeSet();
            };
    private static final Function<PedestalRecipe, Extras> EXTRACT_EXTRAS =
            recipe ->
                    new Extras(
                            Arrays.stream(recipe.cells).boxed().toList(),
                            recipe.extra,
                            recipe.recipeSet);
    public static final MapCodec<PedestalRecipe> MAP_CODEC =
            GenericRecipe.gridCodec(
                    PedestalRecipe::new,
                    (recipe, cells) -> recipe.setCells(cells),
                    recipe -> recipe.cells,
                    EXTRAS_MAP_CODEC,
                    (recipe, extras) -> {
                        recipe.extra = extras.extra();
                        recipe.recipeSet = extras.recipeSet();
                    },
                    EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, PedestalRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    PedestalRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<PedestalRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public int[] cells = EMPTY_CELLS;
    public ExtraCondition extra = ExtraCondition.NONE;
    public int recipeSet;

    private volatile CountIngredient[] grid;

    public PedestalRecipe(String name) {
        super(name);
    }

    public PedestalRecipe setCells(int... cells) {
        this.cells = cells;
        this.grid = null;
        return this;
    }

    public PedestalRecipe extra(ExtraCondition extra) {
        this.extra = extra;
        return this;
    }

    public PedestalRecipe set(int recipeSet) {
        this.recipeSet = recipeSet;
        return this;
    }

    public ExtraCondition extra() {
        return extra;
    }

    public int recipeSet() {
        return recipeSet;
    }

    public CountIngredient[] input() {
        CountIngredient[] built = grid;
        if (built == null) {
            built = new CountIngredient[GRID_SIZE];
            for (int i = 0; i < cells.length; i++) built[cells[i]] = inputItem[i];
            grid = built;
        }
        return built;
    }

    public ItemStack output() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY).copy();
    }

    public boolean matches(ItemStack[] stacks) {
        CountIngredient[] ingredients = input();
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = stacks[i];
            boolean empty = stack.isEmpty();
            CountIngredient ingredient = ingredients[i];
            if (ingredient == null) {
                if (!empty) return false;
                continue;
            }

            if (empty || stack.getCount() != ingredient.count() || !ingredient.matchesItem(stack))
                return false;
        }
        return true;
    }

    public ItemStack displayStack(int slot, long cycle) {
        CountIngredient ingredient = input()[slot];
        if (ingredient == null) return ItemStack.EMPTY;
        List<ItemStack> choices = ingredient.displayStacks();
        if (choices.isEmpty()) return ItemStack.EMPTY;
        return choices.get((int) (Math.abs(cycle) % choices.size()));
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return PedestalRecipes.INSTANCE;
    }

    private record Extras(List<Integer> cells, ExtraCondition extra, int recipeSet) {}
}

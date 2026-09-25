// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class GasCentrifugeRecipes
        extends GenericRecipes<GasCentrifugeRecipe, GasCentrifugeRecipes.Index> {

    public static final GasCentrifugeRecipes INSTANCE = new GasCentrifugeRecipes();

    @Override
    protected String registryName() {
        return "gas_centrifuge";
    }

    @Override
    protected RecipeSerializer<GasCentrifugeRecipe> serializer() {
        return GasCentrifugeRecipe.SERIALIZER;
    }

    @Override
    protected Index indexRows(List<GasCentrifugeRecipe> rows) {
        Map<String, GasCentrifugeRecipe> byStage = new HashMap<>();
        Map<Fluid, GasCentrifugeRecipe> byFeed = new HashMap<>();
        for (GasCentrifugeRecipe recipe : rows) {
            byStage.put(recipe.stage, recipe);
            if (recipe.feed != null) byFeed.put(recipe.feed, recipe);
        }
        return new Index(Map.copyOf(byStage), Map.copyOf(byFeed));
    }

    public @Nullable GasCentrifugeRecipe byStage(@Nullable String stage) {
        if (stage == null || stage.isEmpty()) return null;
        return index().byStage().get(stage);
    }

    public @Nullable GasCentrifugeRecipe byFeed(@Nullable Fluid feed) {
        if (feed == null) return null;
        return index().byFeed().get(feed);
    }

    public boolean isFeedstock(@Nullable String stage) {
        GasCentrifugeRecipe recipe = byStage(stage);
        return recipe != null && recipe.feed != null;
    }

    public List<GasCentrifugeRecipe> allStages() {
        return List.copyOf(index().byStage().values());
    }

    record Index(
            Map<String, GasCentrifugeRecipe> byStage, Map<Fluid, GasCentrifugeRecipe> byFeed) {}
}

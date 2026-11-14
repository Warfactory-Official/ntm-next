/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.CokerRecipe;
import com.hbm.inventory.recipes.CokerRecipes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class CokerPage extends RecipePage<CokerPage.Recipe> {

    CokerPage() {
        super(PageIds.page("coker"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        return CokerRecipes.INSTANCE.byInputFluid().reachable().stream().map(Recipe::new).toList();
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.recipe().recipeId().identifier();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_coker");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_COKER);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_COKER));
    }

    @Override
    public void layout(Recipe wrapped, PageLayout page) {
        CokerRecipe recipe = wrapped.recipe();
        page.input(48, 24).background().fluid(wrapped.input(), recipe.fillReq());
        page.output(102, 24).background().item(recipe.output());
        FluidStackNTM byproduct = recipe.byproduct();
        if (byproduct != null) page.output(120, 24).background().fluid(byproduct);
        page.catalyst(75, 31).item(ModBlocks.MACHINE_COKER.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Fluid input, CokerRecipe recipe) {
        public Recipe(CokerRecipe recipe) {
            this(recipe.inputFluid[0].type(), recipe);
        }
    }
}

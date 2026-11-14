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

import com.hbm.blockentity.machine.ExposureChamberBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.ExposureChamberRecipe;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ExposureChamberPage extends TablePage<ExposureChamberRecipe> {
    private static final int PARTICLE_X = 30, PARTICLE_Y = 24;
    private static final int INGREDIENT_X = 48, INGREDIENT_Y = 24;
    private static final int OUTPUT_X = 102, OUTPUT_Y = 24;

    private static final int BATCH = ExposureChamberBlockEntity.CHARGES_PER_CAPSULE;

    ExposureChamberPage() {
        super(PageIds.page("exposure_chamber"), ExposureChamberRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_exposure_chamber");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_EXPOSURE_CHAMBER);
    }

    @Override
    public List<ExposureChamberRecipe> rows() {
        return ExposureChamberRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_EXPOSURE_CHAMBER));
    }

    @Override
    public void layout(ExposureChamberRecipe recipe, PageLayout page) {
        page.input(PARTICLE_X, PARTICLE_Y).background().items(recipe.particle().displayStacks());
        page.input(INGREDIENT_X, INGREDIENT_Y)
                .background()
                .items(CountIngredient.displayStacks(recipe.ingredient().ingredient(), BATCH));
        page.output(OUTPUT_X, OUTPUT_Y).background().item(recipe.output().copyWithCount(BATCH));
        page.catalyst(75, 31).item(ModBlocks.MACHINE_EXPOSURE_CHAMBER.get());
    }

    @Override
    public void draw(ExposureChamberRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }
}

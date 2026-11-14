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
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class AnvilSmithingPage extends TablePage<AnvilSmithingRecipe> {

    AnvilSmithingPage() {
        super(PageIds.page("anvil_smithing"), AnvilSmithingRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("desc.misc.anvilConstructionCategory.anvil");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.ANVIL_IRON);
    }

    @Override
    public List<AnvilSmithingRecipe> rows() {
        return AnvilSmithingRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return AnvilConstructionPage.anvils();
    }

    @Override
    public void layout(AnvilSmithingRecipe recipe, PageLayout page) {
        page.input(39, 24).items(recipe.left().displayStacks());
        page.input(75, 24).items(recipe.right().displayStacks());
        page.output(111, 24).item(recipe.getSimpleOutput());
    }

    @Override
    public void draw(AnvilSmithingRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.SMITHING);
        RecipePanel.leftAligned(
                graphics,
                Component.translatable("jei.hbm.anvil_tier", recipe.tier),
                52,
                43,
                RecipePanel.TEXT);
    }
}

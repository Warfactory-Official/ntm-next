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

import com.hbm.blocks.generic.ToolConversionBlock;
import com.hbm.integration.recipeviewer.GenericMachinePage;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class ToolingPage extends RecipePage<ToolingPage.Recipe> {

    ToolingPage() {
        super(PageIds.page("tooling"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> rows = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof ToolConversionBlock conversion)) continue;
            List<List<ItemStack>> inputs = new ArrayList<>();
            for (CountIngredient cost : conversion.cost()) inputs.add(cost.displayStacks());
            inputs.add(List.of(new ItemStack(block)));
            List<ItemStack> tools =
                    conversion.tool().getItems().stream().map(ItemStack::new).toList();
            rows.add(
                    new Recipe(
                            PageIds.derived(
                                    id(), PageIds.segment(BuiltInRegistries.BLOCK.getKey(block))),
                            inputs,
                            new ItemStack(conversion.result()),
                            tools));
        }
        return rows;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.tooling");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModItems.BOLTGUN);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModItems.BOLTGUN),
                new ItemStack(ModItems.BLOWTORCH),
                new ItemStack(ModItems.ACETYLENE_TORCH));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        List<List<ItemStack>> inputs = recipe.inputs();
        int[][] positions = GenericMachinePage.universalInputPositions(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            page.input(positions[i][0], positions[i][1]).background().items(inputs.get(i));
        }
        page.output(102, 24).background().item(recipe.output());
        page.catalyst(75, 31).items(recipe.tools());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            Identifier id, List<List<ItemStack>> inputs, ItemStack output, List<ItemStack> tools) {}
}

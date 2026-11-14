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
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.items.machine.ScrapsItem;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class CastingPage extends RecipePage<CastingPage.Recipe> {

    CastingPage() {
        super(PageIds.page("casting"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        return CrucibleRecipes.moldRecipes().stream()
                .map(c -> new Recipe(rowId(c), c.metal(), c.mold(), c.block(), c.result()))
                .toList();
    }

    private Identifier rowId(CrucibleRecipes.Cast cast) {
        return PageIds.derived(
                id(),
                PageIds.segment(BuiltInRegistries.ITEM.getKey(cast.mold().getItem())),
                ScrapsItem.getMats(cast.metal()).material.name());
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.casting");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_STRAND_CASTER);
    }

    @Override
    public int height() {
        return 60;
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModBlocks.FOUNDRY_BASIN),
                new ItemStack(ModBlocks.FOUNDRY_MOLD),
                new ItemStack(ModBlocks.MACHINE_STRAND_CASTER));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).item(recipe.metal());
        page.input(75, 6).item(recipe.mold());
        page.input(75, 42).item(recipe.block());
        page.output(102, 24).item(recipe.result());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.FOUNDRY);
    }

    public record Recipe(
            Identifier id, ItemStack metal, ItemStack mold, ItemStack block, ItemStack result) {}
}

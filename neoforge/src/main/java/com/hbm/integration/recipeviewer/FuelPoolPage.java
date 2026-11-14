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
import com.hbm.inventory.recipes.FuelPoolRecipe;
import com.hbm.inventory.recipes.FuelPoolRecipes;
import com.hbm.items.machine.RBMKRodItem;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class FuelPoolPage extends TablePage<FuelPoolRecipe> {

    FuelPoolPage() {
        super(PageIds.page("fuel_pool"), FuelPoolRecipe.class);
    }

    @Override
    public List<FuelPoolRecipe> rows() {
        return FuelPoolRecipes.INSTANCE.recipes();
    }

    @Override
    public LookupFilter lookupFilter() {
        return (looked, lookup) -> {
            if (!(looked.getItem() instanceof RBMKRodItem)) return true;
            boolean hot =
                    RBMKRodItem.getCoreHeat(looked) >= 50 || RBMKRodItem.getHullHeat(looked) >= 50;
            return lookup == Lookup.USES ? hot : !hot;
        };
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.fuel_pool");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.WASTE_DRUM);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.WASTE_DRUM));
    }

    @Override
    public void layout(FuelPoolRecipe recipe, PageLayout page) {
        page.input(48, 24).background().items(recipe.input().displayStacks());
        page.output(102, 24).background().item(recipe.output());
        page.catalyst(75, 31).item(ModBlocks.WASTE_DRUM.get());
    }

    @Override
    public void draw(FuelPoolRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }
}

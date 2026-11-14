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
import com.hbm.integration.recipeviewer.GenericMachinePage;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.recipes.RotaryFurnaceRecipe;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.machine.ScrapsItem;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class RotaryFurnacePage extends TablePage<RotaryFurnaceRecipe> {

    RotaryFurnacePage() {
        super(PageIds.page("rotary_furnace"), RotaryFurnaceRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_rotary_furnace");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_ROTARY_FURNACE);
    }

    @Override
    public List<RotaryFurnaceRecipe> rows() {
        return RotaryFurnaceRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_ROTARY_FURNACE));
    }

    @Override
    public void layout(RotaryFurnaceRecipe recipe, PageLayout page) {
        FluidStackNTM fluid = recipe.fluid();
        int count = recipe.inputItem.length + (fluid == null ? 0 : 1);
        int[][] positions = GenericMachinePage.universalInputPositions(count);
        int slot = 0;
        for (CountIngredient input : recipe.inputItem) {
            page.input(positions[slot][0], positions[slot][1])
                    .background()
                    .items(input.displayStacks());
            slot++;
        }
        if (fluid != null)
            page.input(positions[slot][0], positions[slot][1]).background().fluid(fluid);
        page.output(102, 24).background().item(ScrapsItem.create(recipe.output, true));
        page.catalyst(75, 31).item(ModBlocks.MACHINE_ROTARY_FURNACE.get());
    }

    @Override
    public void draw(RotaryFurnaceRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
        RecipePanel.rightAligned(
                graphics,
                Component.translatable(
                        "jei.hbm.duration", String.format(Locale.ROOT, "%,d", recipe.duration)),
                160,
                43,
                RecipePanel.TEXT);
        RecipePanel.rightAligned(
                graphics,
                Component.translatable(
                        "jei.hbm.fluid_rate",
                        NTMFluidProperties.getDisplayName(NTMFluids.STEAM),
                        String.format(Locale.ROOT, "%,d", recipe.steam)),
                160,
                55,
                RecipePanel.TEXT);
    }
}

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
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipe;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.items.machine.ScrapsItem;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ElectrolyserMetalPage extends TablePage<ElectrolyserMetalRecipe> {

    ElectrolyserMetalPage() {
        super(PageIds.page("electrolyser_metal"), ElectrolyserMetalRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_electrolyser");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_ELECTROLYSER);
    }

    @Override
    public List<ElectrolyserMetalRecipe> rows() {
        return ElectrolyserMetalRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_ELECTROLYSER));
    }

    @Override
    public void layout(ElectrolyserMetalRecipe recipe, PageLayout page) {
        page.input(48, 24).background().items(recipe.inputItem[0].displayStacks());
        page.input(30, 24).background().fluid(NTMFluids.NITRIC_ACID, 100);

        var byproducts = recipe.outputItems();
        int count =
                1 + (recipe.output2 == null ? 0 : 1) + (byproducts == null ? 0 : byproducts.length);
        int[][] positions = GenericMachinePage.outputPositions(count);
        int slot = 0;
        int[] position = positions[slot++];
        page.output(position[0], position[1])
                .background()
                .item(ScrapsItem.create(recipe.output1, true));
        if (recipe.output2 != null) {
            position = positions[slot++];
            page.output(position[0], position[1])
                    .background()
                    .item(ScrapsItem.create(recipe.output2, true));
        }
        if (byproducts != null) {
            for (var output : byproducts) {
                position = positions[slot++];
                page.output(position[0], position[1])
                        .background()
                        .items(GenericMachinePage.outputStacks(output));
            }
        }
        page.catalyst(75, 31).item(ModBlocks.MACHINE_ELECTROLYSER.get());
    }

    @Override
    public void draw(ElectrolyserMetalRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }
}

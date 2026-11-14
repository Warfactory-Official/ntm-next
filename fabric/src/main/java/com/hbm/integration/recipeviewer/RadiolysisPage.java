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
import com.hbm.inventory.recipes.RadiolysisRecipe;
import com.hbm.inventory.recipes.RadiolysisRecipes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

public final class RadiolysisPage extends TablePage<RadiolysisRecipe> {

    RadiolysisPage() {
        super(PageIds.page("radiolysis"), RadiolysisRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_radiolysis");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_RADIOLYSIS);
    }

    @Override
    public List<RadiolysisRecipe> rows() {
        return RadiolysisRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_RADIOLYSIS));
    }

    @Override
    public void layout(RadiolysisRecipe recipe, PageLayout page) {
        page.input(34, 25).fluid(recipe.inputFluid[0]);
        int[] y = {16, 34};
        for (int i = 0; i < recipe.outputFluid.length && i < y.length; i++) {
            FluidStackNTM output = recipe.outputFluid[i];
            if (output.type() == Fluids.EMPTY) continue;
            page.output(118, y[i]).fluid(output);
        }
    }

    @Override
    public void draw(RadiolysisRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.RADIOLYSIS);
    }
}

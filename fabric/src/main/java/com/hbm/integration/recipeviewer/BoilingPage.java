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
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.HeatableTrait;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class BoilingPage extends RecipePage<BoilingPage.Recipe> {

    BoilingPage() {
        super(PageIds.page("boiling"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> rows = new ArrayList<>();
        for (Fluid fluid : NTMFluids.displayOrder()) {
            HeatableTrait trait = NTMFluidProperties.getTrait(fluid, HeatableTrait.class);
            if (trait == null || trait.efficiency(HeatableTrait.Mode.BOILER) <= 0) continue;
            HeatableTrait.Step step = trait.first();
            rows.add(
                    new Recipe(
                            PageIds.derived(
                                    id(), PageIds.segment(BuiltInRegistries.FLUID.getKey(fluid))),
                            fluid,
                            step.consumed(),
                            step.product(),
                            step.produced()));
        }
        return rows;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_boiler");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_BOILER);
    }

    @Override
    public List<ItemStack> catalysts() {
        return machines();
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().fluid(recipe.input(), recipe.inputAmount());
        page.output(102, 24).background().fluid(recipe.output(), recipe.outputAmount());
        page.catalyst(75, 31).items(machines());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    private static List<ItemStack> machines() {
        return List.of(
                new ItemStack(ModBlocks.MACHINE_BOILER),
                new ItemStack(ModBlocks.MACHINE_INDUSTRIAL_BOILER));
    }

    public record Recipe(
            Identifier id, Fluid input, int inputAmount, Fluid output, int outputAmount) {}
}

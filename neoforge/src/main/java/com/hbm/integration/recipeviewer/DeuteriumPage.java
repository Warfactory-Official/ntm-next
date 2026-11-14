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

import com.hbm.blockentity.machine.DeuteriumExtractorBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluids;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class DeuteriumPage extends RecipePage<DeuteriumPage.Recipe> {

    private static final int WATER_PER_ROW = 1_000;

    DeuteriumPage() {
        super(PageIds.page("deuterium"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        return List.of(
                new Recipe(
                        PageIds.derived(
                                id(),
                                PageIds.segment(BuiltInRegistries.FLUID.getKey(NTMFluids.WATER))),
                        NTMFluids.WATER,
                        WATER_PER_ROW,
                        NTMFluids.HEAVYWATER,
                        WATER_PER_ROW / DeuteriumExtractorBlockEntity.INPUT_PER_OUTPUT));
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_deuterium_extractor");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_DEUTERIUM_EXTRACTOR);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModBlocks.MACHINE_DEUTERIUM_EXTRACTOR),
                new ItemStack(ModBlocks.MACHINE_DEUTERIUM_TOWER));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().fluid(recipe.input(), recipe.inputAmount());
        page.output(102, 24).background().fluid(recipe.output(), recipe.outputAmount());
        page.catalyst(75, 31)
                .items(
                        List.of(
                                new ItemStack(ModBlocks.MACHINE_DEUTERIUM_EXTRACTOR),
                                new ItemStack(ModBlocks.MACHINE_DEUTERIUM_TOWER)));
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            Identifier id, Fluid input, int inputAmount, Fluid output, int outputAmount) {}
}

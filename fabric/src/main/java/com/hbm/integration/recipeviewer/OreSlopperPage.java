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

import com.hbm.blockentity.machine.OreSlopperBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.integration.recipeviewer.GenericMachinePage;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.special.BedrockOreBaseItem;
import com.hbm.items.special.BedrockOreKind;
import com.hbm.items.special.BedrockOreNewItem.BedrockOreGrade;
import com.hbm.items.special.BedrockOreNewItem;
import com.hbm.items.special.BedrockOreSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class OreSlopperPage extends RecipePage<OreSlopperPage.Recipe> {

    OreSlopperPage() {
        super(PageIds.page("ore_slopper"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> rows = new ArrayList<>();
        for (ItemStack base : BedrockOreSet.SAMPLES.get().get()) {
            BedrockOreSet set = BedrockOreBaseItem.sample(base).set();
            List<ItemStack> ores = new ArrayList<>();
            for (BedrockOreKind type : set.kinds())
                ores.add(BedrockOreNewItem.make(BedrockOreGrade.BASE, type));
            Identifier id =
                    PageIds.derived(
                            id(), PageIds.segment(BuiltInRegistries.ITEM.getKey(base.getItem())));
            if (set != BedrockOreSet.NTM) id = PageIds.derived(id, set.id());
            rows.add(new Recipe(id, base, ores));
        }
        return rows;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_ore_slopper");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_ORE_SLOPPER);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_ORE_SLOPPER));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        int[][] inputs = GenericMachinePage.universalInputPositions(2);
        page.input(inputs[0][0], inputs[0][1])
                .background()
                .fluid(NTMFluids.WATER, OreSlopperBlockEntity.waterUsed);
        page.input(inputs[1][0], inputs[1][1]).background().item(recipe.input());

        List<ItemStack> ores = recipe.ores();
        int[][] outputs = GenericMachinePage.outputPositions(ores.size() + 1);
        for (int i = 0; i < ores.size(); i++) {
            page.output(outputs[i][0], outputs[i][1]).background().item(ores.get(i));
        }
        page.output(outputs[ores.size()][0], outputs[ores.size()][1])
                .background()
                .fluid(NTMFluids.SLOP, OreSlopperBlockEntity.waterUsed);
        page.catalyst(75, 31).item(ModBlocks.MACHINE_ORE_SLOPPER.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, ItemStack input, List<ItemStack> ores) {}
}

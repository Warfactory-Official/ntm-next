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
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.material.SmeltingBehavior;
import com.hbm.inventory.recipes.ArcFurnaceRecipe;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.machine.ScrapsItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ArcFurnaceMoltenPage extends RecipePage<ArcFurnaceMoltenPage.Row> {

    ArcFurnaceMoltenPage() {
        super(PageIds.page("arc_furnace_fluid"), Row.class);
    }

    @Override
    public List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (ArcFurnaceRecipe recipe : ArcFurnaceRecipes.INSTANCE.pageRows()) {
            MaterialStack[] molten = recipe.fluidOutput();
            if (molten == null || molten.length == 0) continue;
            List<ItemStack> outputs = new ArrayList<>();
            for (MaterialStack stack : molten) outputs.add(ScrapsItem.create(stack, true));
            rows.add(new Row(recipe.recipeId().identifier(), recipe.inputItem[0], null, outputs));
        }
        for (Material mat : MaterialRegistry.ordered()) {
            if (mat.behaviour() != SmeltingBehavior.SMELTABLE) continue;
            MaterialStack ingot = new MaterialStack(mat, MaterialShape.INGOT.amount(1));
            rows.add(
                    new Row(
                            PageIds.derived(id(), mat.name()),
                            null,
                            ScrapsItem.create(ingot, false),
                            List.of(ScrapsItem.create(ingot, true))));
        }
        return rows;
    }

    @Override
    public Identifier rowId(Row row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_arc_furnace");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_ARC_FURNACE);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_ARC_FURNACE));
    }

    @Override
    public void layout(Row row, PageLayout page) {
        page.input(48, 24)
                .background()
                .items(
                        row.ingredient() == null
                                ? List.of(row.scraps())
                                : row.ingredient().displayStacks());
        int[][] positions = GenericMachinePage.outputPositions(row.outputs().size());
        for (int i = 0; i < row.outputs().size(); i++) {
            page.output(positions[i][0], positions[i][1]).background().item(row.outputs().get(i));
        }
        page.catalyst(75, 31).item(ModBlocks.MACHINE_ARC_FURNACE.get());
    }

    @Override
    public void draw(Row row, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Row(
            Identifier id,
            @Nullable CountIngredient ingredient,
            @Nullable ItemStack scraps,
            List<ItemStack> outputs) {}
}

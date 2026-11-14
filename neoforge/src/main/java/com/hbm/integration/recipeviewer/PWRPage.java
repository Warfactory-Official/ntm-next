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
import com.hbm.items.ModItems;
import com.hbm.items.machine.PWRFuelItem.EnumPWRFuel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class PWRPage extends RecipePage<PWRPage.Recipe> {

    PWRPage() {
        super(PageIds.page("pwr"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> out = new ArrayList<>();
        for (EnumPWRFuel fuel : EnumPWRFuel.VALUES) {
            out.add(
                    new Recipe(
                            PageIds.derived(id(), PageIds.segment(fuel)),
                            new ItemStack(ModItems.cold(fuel)),
                            ModItems.PWR_FUEL_HOT.stack(fuel)));
        }
        return out;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.pwr_controller");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.PWR_CONTROLLER);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.PWR_CONTROLLER));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().item(recipe.fuel());
        page.output(102, 24).background().item(recipe.hot());
        page.catalyst(75, 31).item(ModBlocks.PWR_CONTROLLER.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, ItemStack fuel, ItemStack hot) {}
}

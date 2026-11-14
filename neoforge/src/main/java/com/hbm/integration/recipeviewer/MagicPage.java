/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.integration.recipeviewer;

import com.hbm.inventory.recipes.MagicRecipe;
import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModItems;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class MagicPage extends TablePage<MagicRecipe> {

    private static final Identifier SHEET = RegistryUtil.id("textures/gui/processing/gui_book.png");
    private static final int MAX_INPUTS = 4;

    MagicPage() {
        super(PageIds.page("magic"), MagicRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("item.hbm.book_of_");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModItems.BOOK_OF);
    }

    @Override
    public List<MagicRecipe> rows() {
        return MagicRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModItems.BOOK_OF));
    }

    @Override
    public void layout(MagicRecipe recipe, PageLayout page) {
        CountIngredient[] inputs = recipe.inputItem;
        for (int i = 0; i < Math.min(inputs.length, MAX_INPUTS); i++) {
            page.input(25 + (i % 2) * 36, 6 + (i / 2) * 36).items(inputs[i].displayStacks());
        }
        page.output(119, 24).item(recipe.result());
    }

    @Override
    public void draw(MagicRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, SHEET);
    }
}

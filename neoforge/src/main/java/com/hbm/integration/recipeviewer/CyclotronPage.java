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

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.CyclotronRecipe;
import com.hbm.inventory.recipes.CyclotronRecipes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class CyclotronPage extends TablePage<CyclotronRecipe> {
    private static final int PARTICLE_X = 21, PARTICLE_Y = 24;
    private static final int TARGET_X = 75, TARGET_Y = 24;
    private static final int OUTPUT_X = 129, OUTPUT_Y = 24;

    CyclotronPage() {
        super(PageIds.page("cyclotron"), CyclotronRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_cyclotron");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_CYCLOTRON);
    }

    @Override
    public List<CyclotronRecipe> rows() {
        return CyclotronRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_CYCLOTRON));
    }

    @Override
    public void layout(CyclotronRecipe recipe, PageLayout page) {
        page.input(PARTICLE_X, PARTICLE_Y).items(recipe.particle().displayStacks());
        page.input(TARGET_X, TARGET_Y).items(recipe.target().displayStacks());
        page.output(OUTPUT_X, OUTPUT_Y).item(recipe.output());
    }

    @Override
    public void draw(CyclotronRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.CYCLOTRON);
        RecipePanel.progress(graphics, RecipePanel.CYCLOTRON, 44, 24, 100, 119, 24, 16, 48, 0);
    }
}

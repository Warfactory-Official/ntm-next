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
import com.hbm.integration.recipeviewer.GenericMachinePage;
import com.hbm.inventory.recipes.ShredderRecipe;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ShredderPage extends TablePage<ShredderRecipe> {

    ShredderPage() {
        super(PageIds.page("shredder"), ShredderRecipe.class);
    }

    private static List<ItemStack> claimedInputs(ShredderRecipe recipe) {
        List<ItemStack> inputs = new ArrayList<>();
        for (ItemStack stack : recipe.input().displayStacks()) {
            if (ShredderRecipes.getRecipe(stack) == recipe) inputs.add(stack);
        }
        return inputs;
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_shredder");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_SHREDDER);
    }

    @Override
    public List<ShredderRecipe> rows() {
        return ShredderRecipes.INSTANCE.recipes().stream()
                .filter(r -> !claimedInputs(r).isEmpty())
                .toList();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_SHREDDER));
    }

    @Override
    public void layout(ShredderRecipe recipe, PageLayout page) {
        page.input(39, 24).items(claimedInputs(recipe));
        GenericMachinePage.outputSlot(page.output(129, 24), recipe.outputItems()[0]);
        List<ItemStack> blades =
                List.of(
                        new ItemStack(ModItems.BLADES_STEEL.get()),
                        new ItemStack(ModItems.BLADES_TITANIUM.get()),
                        new ItemStack(ModItems.BLADES_DESH.get()));
        page.display(84, 6).items(blades);
        page.display(84, 42).items(blades);
    }

    @Override
    public void draw(ShredderRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.SHREDDER);
        RecipePanel.progress(graphics, RecipePanel.SHREDDER, 3, 6, 36, 86, 16, 52, 480, 7);
        RecipePanel.progress(graphics, RecipePanel.SHREDDER, 80, 23, 100, 118, 24, 16, 48, 0);
    }
}

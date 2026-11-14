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
import com.hbm.inventory.recipes.PressRecipe;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.StampItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class PressPage extends TablePage<PressRecipe> {

    PressPage() {
        super(PageIds.page("press"), PressRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_press");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_PRESS);
    }

    @Override
    public List<PressRecipe> rows() {
        return PressRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModBlocks.MACHINE_PRESS),
                new ItemStack(ModBlocks.MACHINE_EPRESS),
                new ItemStack(ModBlocks.MACHINE_CONVEYOR_PRESS));
    }

    @Override
    public void layout(PressRecipe recipe, PageLayout page) {
        page.input(48, 42).items(recipe.input().displayStacks());

        List<ItemStack> stamps = new ArrayList<>();
        for (Item stamp : StampItem.stampsOf(recipe.type())) stamps.add(new ItemStack(stamp));
        page.input(48, 6).items(stamps);
        page.output(111, 24).item(recipe.output());
    }

    @Override
    public void draw(PressRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.PRESS);
        RecipePanel.progress(graphics, RecipePanel.PRESS, 47, 24, 0, 86, 18, 18, 20, 1);
    }
}

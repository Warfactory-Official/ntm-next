/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.items.ModItems;
import com.hbm.items.machine.RBMKPelletItem;
import com.hbm.items.machine.RBMKRodItem;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RBMKDisassemblyPage extends RecipePage<RBMKDisassemblyPage.Recipe> {

    RBMKDisassemblyPage() {
        super(PageIds.page("rbmk_rod_disassembly"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> rows = new ArrayList<>();
        for (RegistryHandle<RBMKRodItem> handle : ModItems.CRAFTABLE_RODS) {
            RBMKRodItem rod = handle.get();
            RBMKPelletItem pellet = rod.pellet;
            for (int enrichment = 0; enrichment <= 4; enrichment++) {
                rows.add(row(rod, pellet, enrichment, false));
                if (pellet.isXenonEnabled()) rows.add(row(rod, pellet, enrichment, true));
            }
        }
        return rows;
    }

    private Recipe row(RBMKRodItem rod, RBMKPelletItem pellet, int enrichment, boolean poison) {
        String key = PageIds.segment(BuiltInRegistries.ITEM.getKey(rod));
        Identifier id =
                poison
                        ? PageIds.derived(id(), key, String.valueOf(enrichment), "xenon")
                        : PageIds.derived(id(), key, String.valueOf(enrichment));
        ItemStack pellets = pellet.of(new RBMKPelletItem.Stage(enrichment, poison));
        pellets.setCount(8);
        ItemStack stack = new ItemStack(rod);
        RBMKRodItem.setYield(stack, Math.min(1 - enrichment / 5D, 0.99) * rod.yield);
        if (poison) RBMKRodItem.setPoison(stack, 50);
        return new Recipe(id, stack, pellets);
    }

    @Override
    public LookupFilter lookupFilter() {
        return (looked, lookup) ->
                lookup != Lookup.USES
                        || !(looked.getItem() instanceof RBMKRodItem)
                        || RBMKRodItem.getCoreHeat(looked) <= 50
                                && RBMKRodItem.getHullHeat(looked) <= 50;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.rbmk_rod_disassembly");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.CRAFTING_TABLE);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(Items.CRAFTING_TABLE));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().item(recipe.rod());
        page.output(102, 24).background().item(recipe.pellets());
        page.catalyst(75, 31).item(Items.CRAFTING_TABLE);
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, ItemStack rod, ItemStack pellets) {}
}

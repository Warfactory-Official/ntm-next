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

import com.hbm.blockentity.machine.thermal.TimberSawBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.integration.recipeviewer.GenericMachinePage;
import com.hbm.inventory.material.MaterialTagRegistry;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SawmillPage extends RecipePage<SawmillPage.Recipe> {

    SawmillPage() {
        super(PageIds.page("sawmill"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        return List.of(
                row(ItemTags.LOGS, new ItemStack(Items.OAK_PLANKS, 6)),
                row(ItemTags.PLANKS, new ItemStack(Items.STICK, 6)),
                row(ItemTags.SAPLINGS, new ItemStack(Items.STICK, 1)),
                row(MaterialTagRegistry.KEY_STICK, new ItemStack(ModItems.POWDER_SAWDUST)));
    }

    private Recipe row(TagKey<Item> tag, ItemStack output) {
        List<ItemStack> input = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(item -> input.add(new ItemStack(item)));
        return new Recipe(
                PageIds.derived(id(), PageIds.segment(tag.location())),
                input,
                output,
                TimberSawBlockEntity.sawdustChance(output));
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_sawmill");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_SAWMILL);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_SAWMILL));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        boolean byproduct = recipe.sawdustChance() > 0F;
        int[][] outputs = GenericMachinePage.outputPositions(byproduct ? 2 : 1);
        page.input(48, 24).background().items(recipe.input());
        page.output(outputs[0][0], outputs[0][1]).background().item(recipe.output());
        if (byproduct) {
            page.output(outputs[1][0], outputs[1][1])
                    .background()
                    .item(ModItems.POWDER_SAWDUST.get())
                    .tooltip(
                            (shown, lines) ->
                                    lines.accept(
                                            Component.translatable(
                                                            "jei.hbm.sawmill_chance",
                                                            Math.round(
                                                                    recipe.sawdustChance() * 100F))
                                                    .withStyle(ChatFormatting.RED)));
        }
        page.catalyst(75, 31).item(ModBlocks.MACHINE_SAWMILL.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            Identifier id, List<ItemStack> input, ItemStack output, float sawdustChance) {}
}

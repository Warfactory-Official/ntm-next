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

import com.hbm.blocks.ModBlocks;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModItems;
import com.hbm.saveddata.satellites.SatelliteType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SatellitePage extends RecipePage<SatellitePage.Recipe> {

    private static final int OUT_LINE = 6;
    private static final int OUT_SLOTS = 18;

    SatellitePage() {
        super(PageIds.page("satellite"), Recipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.satellite");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.SAT_DOCK.get());
    }

    @Override
    public List<Recipe> rows() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return List.of();
        List<Recipe> rows = new ArrayList<>();
        for (Item satellite :
                List.of(
                        ModItems.SATELLITE_MINER_ASTRO.get(),
                        ModItems.SATELLITE_MINER_LUNAR.get(),
                        ModItems.SAT_MINER.get(),
                        ModItems.SAT_LUNAR_MINER.get())) {
            rows.add(
                    new Recipe(
                            PageIds.derived(
                                    id(),
                                    PageIds.segment(BuiltInRegistries.ITEM.getKey(satellite))),
                            new ItemStack(satellite),
                            ItemPool.read(
                                    connection.registryAccess(),
                                    SatelliteType.cargoFor(satellite))));
        }
        return rows;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.SAT_DOCK.get()));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(12, 24).item(recipe.satellite());

        int weight = 0;
        for (ItemPool.Entry entry : recipe.pool()) weight += entry.weight();
        List<ItemStack> outs = new ArrayList<>();
        List<Float> chances = new ArrayList<>();
        for (ItemPool.Entry entry : recipe.pool()) {
            if (entry.isEmpty()) continue;
            outs.add(new ItemStack(entry.item(), 1, entry.patch()));
            chances.add(100F * entry.weight() / weight);
        }

        int overflow = outs.size() / OUT_SLOTS;
        for (int i = 0; i < Math.min(outs.size(), OUT_SLOTS); i++) {
            List<ItemStack> stacks = new ArrayList<>();
            List<Float> odds = new ArrayList<>();
            for (int j = 0; j < overflow + 1 && j * OUT_SLOTS + i < outs.size(); j++) {
                stacks.add(outs.get(j * OUT_SLOTS + i));
                odds.add(chances.get(j * OUT_SLOTS + i));
            }
            page.output(48 + 18 * (i % OUT_LINE), 6 + 18 * (i / OUT_LINE))
                    .items(stacks)
                    .tooltip(
                            (shown, lines) -> {
                                if (shown == null) return;
                                for (int k = 0; k < stacks.size(); k++) {
                                    if (!ItemStack.isSameItemSameComponents(shown, stacks.get(k)))
                                        continue;
                                    lines.accept(
                                            Component.translatable(
                                                            "jei.hbm.satellite_chance",
                                                            (int) (odds.get(k) * 10F) / 10F)
                                                    .withStyle(ChatFormatting.RED));
                                    return;
                                }
                            });
        }

        page.catalyst(30, 31).item(ModBlocks.SAT_DOCK.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.region(graphics, RecipePanel.ANVIL, 11, 23, 113, 105, 18, 18);
        RecipePanel.region(graphics, RecipePanel.ANVIL, 47, 5, 5, 87, 108, 54);
        RecipePanel.region(graphics, RecipePanel.ANVIL, 29, 14, 131, 96, 18, 36);
    }

    public record Recipe(Identifier id, ItemStack satellite, List<ItemPool.Entry> pool) {}
}

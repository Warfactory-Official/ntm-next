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
import com.hbm.blocks.machine.AnvilBlock;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe.AnvilOutput;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class AnvilConstructionPage extends TablePage<AnvilConstructionRecipe> {

    AnvilConstructionPage() {
        super(PageIds.page("anvil_construction"), AnvilConstructionRecipe.class);
    }

    static List<ItemStack> anvils() {
        List<ItemStack> anvils = new ArrayList<>();
        for (RegistryHandle<AnvilBlock> anvil : ModBlocks.ANVILS)
            anvils.add(new ItemStack(anvil.get()));
        return anvils;
    }

    private static Shape shapeOf(int in, int out) {
        if (in == 1 && out == 1) return Shape.SMITHING;
        if (in == 1) return Shape.RECYCLING;
        if (out == 1) return Shape.CONSTRUCTION;
        return Shape.NONE;
    }

    @Override
    public Component title() {
        return Component.translatable("desc.misc.anvilConstructionCategory.anvil");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.ANVIL_IRON);
    }

    @Override
    public List<AnvilConstructionRecipe> rows() {
        return AnvilConstructionRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return anvils();
    }

    @Override
    public void layout(AnvilConstructionRecipe recipe, PageLayout page) {
        List<CountIngredient> inputs = recipe.input();
        List<AnvilOutput> outputs = recipe.outputs();
        int in = inputs.size();
        int out = outputs.size();
        Shape shape = shapeOf(in, out);
        int inLine = shape.inLine, outLine = shape.outLine;
        int inOX = shape.inOX, inOY = shape.inOY, outOX = shape.outOX, outOY = shape.outOY;
        int anvX = shape.anvX, anvY = 31;

        for (int i = 0; i < in; i++) {
            page.input(inOX + 18 * (i % inLine), inOY + 18 * (i / inLine))
                    .items(inputs.get(i).displayStacks());
        }

        for (int i = 0; i < out; i++) {
            AnvilOutput output = outputs.get(i);
            PageSlot slot =
                    page.output(outOX + 18 * (i % outLine), outOY + 18 * (i / outLine))
                            .item(output.stack());
            if (output.chance() != 1F) {
                slot.tooltip(
                        (shown, lines) ->
                                lines.accept(
                                        Component.literal(
                                                        ((int) (output.chance() * 1000)) / 10D
                                                                + "%")
                                                .withStyle(ChatFormatting.RED)));
            }
        }

        List<ItemStack> anvils = new ArrayList<>();
        for (RegistryHandle<AnvilBlock> anvil : ModBlocks.ANVILS) {
            if (anvil.get().tier == recipe.tierLower) anvils.add(new ItemStack(anvil.get()));
        }
        if (anvils.isEmpty()) {
            anvils.add(new ItemStack(ModBlocks.ANVIL_IRON));
        }
        page.display(anvX, anvY).items(anvils);
    }

    @Override
    public void draw(AnvilConstructionRecipe recipe, GuiGraphicsExtractor graphics) {
        shapeOf(recipe.input().size(), recipe.outputs().size()).draw(graphics);
    }

    private enum Shape {
        SMITHING(1, 1, 48, 24, 102, 24, 75) {
            @Override
            void draw(GuiGraphicsExtractor graphics) {
                region(graphics, 47, 23, 113, 105, 18, 18);
                region(graphics, 101, 23, 113, 105, 18, 18);
                region(graphics, 74, 14, 149, 96, 18, 36);
            }
        },
        RECYCLING(1, 6, 12, 24, 48, 6, 30) {
            @Override
            void draw(GuiGraphicsExtractor graphics) {
                region(graphics, 11, 23, 113, 105, 18, 18);
                region(graphics, 47, 5, 5, 87, 108, 54);
                region(graphics, 29, 14, 185, 96, 18, 36);
            }
        },
        CONSTRUCTION(6, 1, 12, 6, 138, 24, 120) {
            @Override
            void draw(GuiGraphicsExtractor graphics) {
                region(graphics, 11, 5, 5, 87, 108, 54);
                region(graphics, 137, 23, 113, 105, 18, 18);
                region(graphics, 119, 14, 167, 96, 18, 36);
            }
        },
        NONE(4, 4, 3, 6, 93, 6, 75) {
            @Override
            void draw(GuiGraphicsExtractor graphics) {
                region(graphics, 2, 5, 5, 87, 72, 54);
                region(graphics, 92, 5, 5, 87, 72, 54);
                region(graphics, 74, 14, 131, 96, 18, 36);
            }
        };

        final int inLine;
        final int outLine;
        final int inOX;
        final int inOY;
        final int outOX;
        final int outOY;
        final int anvX;

        Shape(int inLine, int outLine, int inOX, int inOY, int outOX, int outOY, int anvX) {
            this.inLine = inLine;
            this.outLine = outLine;
            this.inOX = inOX;
            this.inOY = inOY;
            this.outOX = outOX;
            this.outOY = outOY;
            this.anvX = anvX;
        }

        static void region(
                GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
            RecipePanel.region(graphics, RecipePanel.ANVIL, x, y, u, v, width, height);
        }

        abstract void draw(GuiGraphicsExtractor graphics);
    }
}

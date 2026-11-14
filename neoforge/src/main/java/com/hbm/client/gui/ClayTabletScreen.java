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

package com.hbm.client.gui;

import com.hbm.inventory.recipes.PedestalRecipe;
import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.special.ClayTabletItem;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ClayTabletScreen extends Screen {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/guide_pedestal.png");
    private static final int WIDTH = 142;
    private static final int HEIGHT = 84;

    private final Player player;
    private int left;
    private int top;

    public ClayTabletScreen(Player player) {
        super(Component.translatable("item.hbm.clay_tablet"));
        this.player = player;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        ItemStack tablet = player.getMainHandItem();
        int variant = tablet.getItem() instanceof ClayTabletItem item ? item.recipeSet : 0;
        int tabletOffset = variant == 1 ? 84 : 0;
        int iconOffset = variant == 1 ? 16 : 0;
        float revealChance = variant == 1 ? 0.25F : 0.5F;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left,
                top,
                0.0F,
                tabletOffset,
                WIDTH,
                HEIGHT,
                256,
                256);

        List<PedestalRecipe> recipes = PedestalRecipes.recipeSet(variant);
        if (!(tablet.getItem() instanceof ClayTabletItem)
                || !tablet.has(ModDataComponents.TABLET_SEED.get())
                || recipes.isEmpty()) {
            coveredGrid(graphics, iconOffset);
            return;
        }

        Random random = new Random(tablet.get(ModDataComponents.TABLET_SEED.get()));
        PedestalRecipe recipe = recipes.get(random.nextInt(recipes.size()));
        switch (recipe.extra()) {
            case FULL_MOON -> icon(graphics, iconOffset, 32);
            case NEW_MOON -> icon(graphics, iconOffset, 48);
            case SUN -> icon(graphics, iconOffset, 64);
            default -> {}
        }

        long cycle = minecraft.gui.hud.getGuiTicks() / 20L;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int x = left + 7 + column * 27;
                int y = top + 7 + row * 27;
                if (random.nextFloat() > revealChance) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            x,
                            y,
                            142.0F + iconOffset,
                            16.0F,
                            16,
                            16,
                            256,
                            256);
                    continue;
                }
                ItemStack input = recipe.displayStack(column + row * 3, cycle);
                if (input.isEmpty()) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            x,
                            y,
                            142.0F + iconOffset,
                            0.0F,
                            16,
                            16,
                            256,
                            256);
                } else {
                    graphics.item(input, x, y);
                    graphics.itemDecorations(
                            font,
                            input,
                            x,
                            y,
                            input.getCount() > 1 ? Integer.toString(input.getCount()) : null);
                }
            }
        }

        ItemStack output = recipe.output();
        if (!output.isEmpty()) {
            int x = left + WIDTH / 2 - 8;
            graphics.item(output, x, top - 20);
            graphics.itemDecorations(
                    font,
                    output,
                    x,
                    top - 20,
                    output.getCount() > 1 ? Integer.toString(output.getCount()) : null);
            String label = output.getHoverName().getString();
            graphics.text(
                    font,
                    label,
                    left + (WIDTH - font.width(label)) / 2,
                    top - 30,
                    0xFFFFFFFF,
                    false);
        }
    }

    private void coveredGrid(GuiGraphicsExtractor graphics, int iconOffset) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        left + 7 + column * 27,
                        top + 7 + row * 27,
                        142.0F + iconOffset,
                        16.0F,
                        16,
                        16,
                        256,
                        256);
            }
        }
    }

    private void icon(GuiGraphicsExtractor graphics, int iconOffset, int v) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left + 120,
                top + 62,
                142.0F + iconOffset,
                v,
                16,
                16,
                256,
                256);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

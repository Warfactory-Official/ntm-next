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

import com.hbm.util.GameTime;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class RecipePanel {

    public static final int WIDTH = 166;
    public static final int HEIGHT = 65;
    public static final int SIDE = 164;
    public static final int TEXT = 0xFF404040;

    public static final Identifier GENERIC = sheet("gui_nei");
    public static final Identifier ANVIL = sheet("gui_nei_anvil");
    public static final Identifier CRUCIBLE = sheet("gui_nei_crucible");
    public static final Identifier CRUCIBLE_SMELTING = sheet("gui_nei_crucible_smelting");
    public static final Identifier FOUNDRY = sheet("gui_nei_foundry");
    public static final Identifier CUSTOM = sheet("gui_nei_custom");
    public static final Identifier CYCLOTRON = sheet("gui_nei_cyclotron");
    public static final Identifier PRESS = sheet("gui_nei_press");
    public static final Identifier RADIOLYSIS = sheet("gui_nei_radiolysis");
    public static final Identifier REFINERY = sheet("gui_nei_refinery");
    public static final Identifier SHREDDER = sheet("gui_nei_shredder");
    public static final Identifier SILEX = sheet("gui_nei_silex");
    public static final Identifier SMITHING = sheet("gui_nei_smithing");

    private RecipePanel() {}

    private static Identifier sheet(String name) {
        return RegistryUtil.id("textures/gui/nei/" + name + ".png");
    }

    public static void page(GuiGraphicsExtractor graphics, Identifier sheet) {
        region(graphics, sheet, 0, 0, 5, 11, WIDTH, HEIGHT);
    }

    public static void region(
            GuiGraphicsExtractor graphics,
            Identifier sheet,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, u, v, width, height, 256, 256);
    }

    public static void universalPage(GuiGraphicsExtractor graphics) {
        operation(graphics, 74);
    }

    public static void operation(GuiGraphicsExtractor graphics, int x) {
        region(graphics, GENERIC, x, 14, 59, 87, 18, 36);
    }

    public static void operationWithTemplate(GuiGraphicsExtractor graphics, int x) {
        region(graphics, GENERIC, x, 7, 77, 87, 18, 50);
    }

    public static void progress(
            GuiGraphicsExtractor graphics,
            Identifier sheet,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height,
            int ticks,
            int direction) {
        float completion = GameTime.ticks() % ticks / (float) ticks;
        if (direction > 3) {
            completion = 1 - completion;
            direction %= 4;
        }
        int shown = (int) (completion * (direction % 2 == 0 ? width : height));
        switch (direction) {
            case 0 -> region(graphics, sheet, x, y, u, v, shown, height);
            case 1 -> region(graphics, sheet, x, y, u, v, width, shown);
            case 2 ->
                    region(
                            graphics,
                            sheet,
                            x + width - shown,
                            y,
                            u + width - shown,
                            v,
                            shown,
                            height);
            default ->
                    region(
                            graphics,
                            sheet,
                            x,
                            y + height - shown,
                            u,
                            v + height - shown,
                            width,
                            shown);
        }
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static void rightAligned(
            GuiGraphicsExtractor graphics, Component text, int y, int colour) {
        rightAligned(graphics, text, SIDE, y, colour);
    }

    public static void rightAligned(
            GuiGraphicsExtractor graphics, Component text, int side, int y, int colour) {
        Font font = font();
        graphics.text(font, text, side - font.width(text), y, colour, false);
    }

    public static void leftAligned(
            GuiGraphicsExtractor graphics, Component text, int x, int y, int colour) {
        graphics.text(font(), text, x, y, colour, false);
    }

    public static void centred(
            GuiGraphicsExtractor graphics, Component text, int centre, int y, int colour) {
        Font font = font();
        graphics.text(font, text, centre - font.width(text) / 2, y, colour, false);
    }
}

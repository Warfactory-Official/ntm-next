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

package com.hbm.client.gui;

import com.hbm.inventory.container.NukeMikeMenu;
import com.hbm.items.ModItems;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public class NukeMikeScreen extends InfoContainerScreen<NukeMikeMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/ivymikeschematic.png");

    private static final int[][] LENS = {
        {24, 20, 209, 1}, {24, 43, 209, 24}, {47, 20, 232, 1}, {47, 43, 232, 24}
    };

    public NukeMikeScreen(NukeMikeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 217);
        this.titleLabelY = 4;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        if (menu.isReady() && !menu.isFilled()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 5, 35, 177.0F, 1.0F, 16, 16, 256, 256);
        }
        if (menu.isReady() && menu.isFilled()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 5, 35, 177.0F, 19.0F, 16, 16, 256, 256);
        }

        part(graphics, 5, ModItems.MIKE_CORE.get(), 75, 25, 176, 49, 80, 36);
        part(graphics, 6, ModItems.MIKE_DEUT.get(), 79, 30, 180, 88, 58, 26);
        part(graphics, 7, ModItems.MIKE_COOLING_UNIT.get(), 140, 30, 240, 88, 12, 26);

        for (int i = 0; i < LENS.length; i++) {
            int[] r = LENS[i];
            part(graphics, i, ModItems.EXPLOSIVE_LENSES.get(), r[0], r[1], r[2], r[3], 23, 23);
        }

        drawInfoPanel(graphics, -16, 16, 16, 16, 2);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                -16,
                16,
                16,
                16,
                -8,
                32,
                lineArray("desc.gui.nukeMike.desc"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void part(
            GuiGraphicsExtractor graphics,
            int slot,
            Item item,
            int x,
            int y,
            int u,
            int v,
            int w,
            int h) {
        if (!menu.part(slot).is(item)) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, w, h, 256, 256);
    }
}

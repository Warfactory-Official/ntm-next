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

import com.hbm.inventory.container.NukeTsarMenu;
import com.hbm.items.ModItems;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public class NukeTsarScreen extends InfoContainerScreen<NukeTsarMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/tsarbombaschematic.png");
    private static final Identifier MIKE =
            RegistryUtil.id("textures/gui/weapon/ivymikeschematic.png");

    private static final int[][] LENS = {
        {40, 36, 209, 1}, {40, 59, 209, 24}, {63, 36, 232, 1}, {63, 59, 232, 24}
    };

    public NukeTsarScreen(NukeTsarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 256, 233);
        this.inventoryLabelX = 48;
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

        if (menu.isFilled()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, MIKE, 18, 50, 176.0F, 18.0F, 16, 16, 256, 256);
        } else if (menu.isReady()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, MIKE, 18, 50, 176.0F, 0.0F, 16, 16, 256, 256);
        }

        for (int i = 0; i < LENS.length; i++) {
            int[] r = LENS[i];
            part(graphics, i, ModItems.EXPLOSIVE_LENSES.get(), r[0], r[1], r[2], r[3], 23, 23);
        }
        part(graphics, 5, ModItems.TSAR_CORE.get(), 91, 41, 176, 220, 80, 36);

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
                lineArray("desc.gui.nukeTsar.desc"));

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
        graphics.blit(RenderPipelines.GUI_TEXTURED, MIKE, x, y, u, v, w, h, 256, 256);
    }
}

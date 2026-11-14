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

import com.hbm.blockentity.machine.OreSlopperBlockEntity;
import com.hbm.inventory.container.OreSlopperMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class OreSlopperScreen extends InfoContainerScreen<OreSlopperMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_ore_slopper.png");

    public OreSlopperScreen(OreSlopperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 9;
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

        OreSlopperBlockEntity be = slopper();

        int i = (int) (be.progress * 35);
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    62,
                    52 - i,
                    176,
                    34 - i,
                    34,
                    i,
                    256,
                    256);

        int j = (int) (be.power * 52 / OreSlopperBlockEntity.maxPower);
        if (j > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 70 - j, 176, 86 - j, 16, j, 256, 256);

        if (be.power >= be.consumption) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 12, 4, 202, 34, 9, 12, 256, 256);
        }

        drawFluidBar(graphics, 26, 18, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 116, 18, 16, 52, be.tanks[1]);

        drawElectricityInfo(
                graphics, mouseX, mouseY, 8, 18, 16, 52, be.power, OreSlopperBlockEntity.maxPower);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 26, 18, 34, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 116, 18, 16, 52, be.tanks[1]);

        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2 - 9;
        super.extractLabels(graphics, mouseX, mouseY);
    }

    private OreSlopperBlockEntity slopper() {
        return menu.blockEntity();
    }
}

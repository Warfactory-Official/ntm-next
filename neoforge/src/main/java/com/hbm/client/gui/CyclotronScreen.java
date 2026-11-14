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

import com.hbm.blockentity.machine.CyclotronBlockEntity;
import com.hbm.inventory.container.CyclotronMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class CyclotronScreen extends InfoContainerScreen<CyclotronMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_cyclotron.png");

    private static final int POWER_X = 168, POWER_Y = 18, POWER_W = 16, POWER_H = 63;
    private static final int ARROW_X = 48, ARROW_Y = 27, ARROW_W = 34, ARROW_H = 34;
    private static final int WATER_X = 11, WATER_Y = 81, STEAM_Y = 90, TANK_W = 34, TANK_H = 7;
    private static final int AMAT_X = 107, AMAT_Y = 81, AMAT_W = 34, AMAT_H = 16;
    private static final int PANEL_X = 49, PANEL_Y = 85, PANEL_W = 8, PANEL_H = 8;

    public CyclotronScreen(CyclotronMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 190, 215);
        this.inventoryLabelX = 15;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 79;
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

        CyclotronBlockEntity be = menu.blockEntity();

        int p = menu.getPowerScaled(POWER_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    80 - p,
                    190.0F,
                    62.0F - p,
                    POWER_W,
                    p,
                    256,
                    256);
        }

        int arrow = menu.getProgressScaled(ARROW_W);
        if (arrow > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ARROW_X,
                    ARROW_Y,
                    206.0F,
                    0.0F,
                    arrow,
                    ARROW_H,
                    256,
                    256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 172, 4, 190.0F, 63.0F, 9, 12, 256, 256);
        }

        drawFluidBarH(graphics, WATER_X, WATER_Y, TANK_W, TANK_H, be.water);
        drawFluidBarH(graphics, WATER_X, STEAM_Y, TANK_W, TANK_H, be.steam);
        drawFluidBarH(graphics, AMAT_X, AMAT_Y, AMAT_W, AMAT_H, be.amat);

        drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 8);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y,
                PANEL_W,
                PANEL_H,
                resolveLines(
                        "desc.gui.upgrade",
                        "desc.gui.upgrade.speed",
                        "desc.gui.upgrade.effectiveness",
                        "desc.gui.upgrade.power"));

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                CyclotronBlockEntity.MAX_POWER);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, WATER_X, WATER_Y, TANK_W, TANK_H, be.water);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, WATER_X, STEAM_Y, TANK_W, TANK_H, be.steam);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, AMAT_X, AMAT_Y, AMAT_W, AMAT_H, be.amat);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

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

import com.hbm.blockentity.processing.ShredderBlockEntity;
import com.hbm.inventory.container.ShredderMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ShredderScreen extends InfoContainerScreen<ShredderMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_shredder.png");

    private static final int POWER_X = 8, POWER_H = 88, POWER_W = 16, POWER_BOTTOM = 106;
    private static final int POWER_U = 176, POWER_V_BOTTOM = 160;
    private static final int PROGRESS_X = 63, PROGRESS_Y = 89, PROGRESS_U = 176, PROGRESS_V = 54;
    private static final int PROGRESS_LENGTH = 34;
    private static final int GEAR_LEFT_X = 43, GEAR_RIGHT_X = 79, GEAR_Y = 71;
    private static final int GEAR_LEFT_U = 176, GEAR_RIGHT_U = 194;
    private static final int PANEL_X = -16, PANEL_Y = 36, PANEL_SIZE = 16;

    public ShredderScreen(ShredderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 233);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 106;
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

        long power = menu.getPower();
        if (power > 0) {
            int filled = (int) menu.getPowerScaled(POWER_H);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_BOTTOM - filled,
                    POWER_U,
                    POWER_V_BOTTOM - filled,
                    POWER_W,
                    filled,
                    256,
                    256);
        }

        int progress = menu.getProgressScaled(PROGRESS_LENGTH);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                PROGRESS_X,
                PROGRESS_Y,
                PROGRESS_U,
                PROGRESS_V,
                progress + 1,
                18,
                256,
                256);

        int left = menu.getGearLeft();
        int right = menu.getGearRight();
        drawGear(graphics, GEAR_LEFT_X, GEAR_LEFT_U, left);
        drawGear(graphics, GEAR_RIGHT_X, GEAR_RIGHT_U, right);

        boolean stalled = left == 0 || left == 3 || right == 0 || right == 3;
        if (stalled) {
            drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_SIZE, PANEL_SIZE, 6);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    PANEL_X,
                    PANEL_Y,
                    PANEL_SIZE,
                    PANEL_SIZE,
                    -8,
                    PANEL_Y + PANEL_SIZE,
                    List.of(
                            Component.translatable(
                                    "desc.gui.machineShredder.errorShredderBladesAre")));
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_BOTTOM - POWER_H,
                POWER_W,
                POWER_H,
                power,
                ShredderBlockEntity.MAX_POWER);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawGear(GuiGraphicsExtractor graphics, int x, int u, int gear) {
        if (gear == 0) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                GEAR_Y,
                u,
                (gear - 1) * 18,
                18,
                18,
                256,
                256);
    }
}

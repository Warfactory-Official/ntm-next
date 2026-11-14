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

import com.hbm.blockentity.generator.IsotopeGeneratorBlockEntity;
import com.hbm.inventory.container.RTGMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class RTGScreen extends InfoContainerScreen<RTGMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/generators/gui_rtg.png");

    private static final int BAR_H = 51, BAR_W = 16, BAR_BOTTOM = 61, BAR_TOP = 9;
    private static final int HEAT_X = 124, HEAT_U = 176;
    private static final int POWER_X = 146, POWER_U = 192;
    private static final int BAR_V = 10;
    private static final int INFO_X = -12, INFO_Y = 25, INFO_W = 16, INFO_H = 16;
    private static final String PELLET_KEY = "desc.gui.rtg.pelletPower";

    public RTGScreen(RTGMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 188);
        titleLabelY = 7;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 60;
    }

    @Override
    protected int titleColor() {
        return 0xFFA6B5AE;
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

        if (menu.blockEntity().hasHeat()) {
            int i = menu.getHeatScaled(BAR_H);
            if (i > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        HEAT_X,
                        BAR_BOTTOM - i,
                        HEAT_U,
                        BAR_V + (BAR_H - i),
                        BAR_W,
                        i,
                        256,
                        256);
            }
        }

        long power = menu.getPower();
        if (power > 0) {
            int i = (int) menu.getPowerScaled(BAR_H);
            if (i > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        BAR_BOTTOM - i,
                        POWER_U,
                        BAR_V + (BAR_H - i),
                        BAR_W,
                        i,
                        256,
                        256);
            }
        }

        drawInfoPanel(graphics, INFO_X, INFO_Y, INFO_W, INFO_H, 2);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                BAR_TOP,
                BAR_W,
                BAR_H,
                power,
                IsotopeGeneratorBlockEntity.MAX_POWER);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                HEAT_X,
                BAR_TOP,
                BAR_W,
                BAR_H,
                List.of(Component.translatable("desc.gui.rtg.heat", menu.getHeat())));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y,
                INFO_W,
                INFO_H,
                INFO_X + 4,
                INFO_Y + 27,
                pelletLines(PELLET_KEY, IsotopeGeneratorBlockEntity.POWER_PER_HEAT));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

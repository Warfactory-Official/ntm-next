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

import com.hbm.blockentity.machine.RadiolysisBlockEntity;
import com.hbm.inventory.container.RadiolysisMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class RadiolysisScreen extends InfoContainerScreen<RadiolysisMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_radiolysis.png");

    private static final int POWER_X = 8, POWER_Y = 17, POWER_W = 16, POWER_H = 34, POWER_U = 240;
    private static final int IN_X = 61, IN_Y = 17, IN_W = 8, IN_H = 52;
    private static final int OUT_X = 87, OUT_W = 12, OUT_H = 16;
    private static final int OUT1_Y = 17, OUT2_Y = 53;
    private static final int INFO_X = -16, INFO_Y = 16, INFO_STEP = 18, INFO_SIZE = 16;
    private static final String PELLET_KEY = "desc.gui.rtg.pelletPower";

    public RadiolysisScreen(RadiolysisMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 166);
        titleLabelY = 6;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 88;
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

        var chamber = menu.blockEntity();

        long power = menu.getPower();
        int p = (int) (power * POWER_H / RadiolysisBlockEntity.ENERGY_CAPACITY);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - p),
                    POWER_U,
                    POWER_H - p,
                    POWER_W,
                    p,
                    256,
                    256);
        }

        drawFluidBar(graphics, IN_X, IN_Y, IN_W, IN_H, chamber.tank(0));
        drawFluidBar(graphics, OUT_X, OUT1_Y, OUT_W, OUT_H, chamber.tank(1));
        drawFluidBar(graphics, OUT_X, OUT2_Y, OUT_W, OUT_H, chamber.tank(2));

        drawInfoPanel(graphics, INFO_X, INFO_Y, INFO_SIZE, INFO_SIZE, 10);
        drawInfoPanel(graphics, INFO_X, INFO_Y + INFO_STEP, INFO_SIZE, INFO_SIZE, 2);
        drawInfoPanel(graphics, INFO_X, INFO_Y + INFO_STEP * 2, INFO_SIZE, INFO_SIZE, 3);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, IN_X, IN_Y, IN_W, IN_H, chamber.tank(0));
        drawFluidGaugeInfo(graphics, mouseX, mouseY, OUT_X, OUT1_Y, OUT_W, OUT_H, chamber.tank(1));
        drawFluidGaugeInfo(graphics, mouseX, mouseY, OUT_X, OUT2_Y, OUT_W, OUT_H, chamber.tank(2));

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                power,
                RadiolysisBlockEntity.ENERGY_CAPACITY);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y,
                INFO_SIZE,
                INFO_SIZE,
                INFO_X + 8,
                INFO_Y + INFO_SIZE,
                lineArray("desc.gui.radiolysis.desc"));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y + INFO_STEP,
                INFO_SIZE,
                INFO_SIZE,
                INFO_X + 8,
                INFO_Y + INFO_STEP + INFO_SIZE,
                List.of(Component.translatable("desc.gui.rtg.heat", menu.getHeat())));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y + INFO_STEP * 2,
                INFO_SIZE,
                INFO_SIZE,
                INFO_X + 8,
                INFO_Y + INFO_STEP * 2 + INFO_SIZE,
                pelletLines(PELLET_KEY, RadiolysisBlockEntity.ENERGY_PER_HEAT));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

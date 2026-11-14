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

import com.hbm.blockentity.machine.oil.VacuumRefineryBlockEntity;
import com.hbm.inventory.container.VacuumDistillMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class VacuumDistillScreen extends InfoContainerScreen<VacuumDistillMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_vacuum_distill.png");
    private static final int TEX_W = 256, TEX_H = 256;
    private static final int POWER_X = 26, BAR_TOP = 18, BAR_W = 16, BAR_H = 52;
    private static final int[] TANK_X = {44, 80, 98, 116, 134};

    public VacuumDistillScreen(VacuumDistillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 238);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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
                TEX_W,
                TEX_H);

        VacuumRefineryBlockEntity be = distill();
        long power = menu.getPower();

        int j = (int) (power * 54L / VacuumRefineryBlockEntity.MAX_POWER);
        if (j > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    BAR_TOP + (52 - j),
                    176,
                    52 - j,
                    BAR_W,
                    j,
                    TEX_W,
                    TEX_H);
        }

        for (int i = 0; i < 5; i++)
            drawFluidBar(graphics, TANK_X[i], BAR_TOP, BAR_W, BAR_H, be.tanks[i]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                BAR_TOP,
                BAR_W,
                BAR_H,
                power,
                VacuumRefineryBlockEntity.MAX_POWER);
        for (int i = 0; i < 5; i++)
            drawFluidGaugeInfo(
                    graphics, mouseX, mouseY, TANK_X[i], BAR_TOP, BAR_W, BAR_H, be.tanks[i]);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private VacuumRefineryBlockEntity distill() {
        return menu.blockEntity();
    }
}

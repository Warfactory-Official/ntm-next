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

import com.hbm.blockentity.machine.LargeTurbineBlockEntity;
import com.hbm.inventory.container.LargeTurbineMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class LargeTurbineScreen extends InfoContainerScreen<LargeTurbineMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/generators/gui_turbine_large.png");

    private static final int TANK_IN_X = 62,
            TANK_OUT_X = 134,
            TANK_Y = 17,
            TANK_W = 16,
            TANK_H = 52;
    private static final int POWER_X = 123, POWER_Y = 35, POWER_W = 7, POWER_H = 34;
    private static final int ICON_X = 99, ICON_Y = 18, ICON_S = 14;
    private static final int PANEL_X = -16, PANEL_Y = 68, PANEL_SIZE = 16;

    public LargeTurbineScreen(LargeTurbineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return 0xFF404040;
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

        LargeTurbineBlockEntity be = menu.blockEntity();

        Fluid in = be.tanks[0].getTankType();
        int iconV = -1;
        if (in == NTMFluids.STEAM) iconV = 0;
        else if (in == NTMFluids.HOTSTEAM) iconV = 14;
        else if (in == NTMFluids.SUPERHOTSTEAM) iconV = 28;
        else if (in == NTMFluids.ULTRAHOTSTEAM) iconV = 42;
        if (iconV >= 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ICON_X,
                    ICON_Y,
                    183,
                    iconV,
                    ICON_S,
                    ICON_S,
                    256,
                    256);
        }

        int p = (int) be.getPowerScaled(POWER_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - p),
                    176,
                    POWER_H - p,
                    POWER_W,
                    p,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK_IN_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidBar(graphics, TANK_OUT_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_IN_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_OUT_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                LargeTurbineBlockEntity.maxPower);

        if (be.tanks[1].getTankType() == null) {
            drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_SIZE, PANEL_SIZE, 6);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    PANEL_X,
                    PANEL_Y,
                    PANEL_SIZE,
                    PANEL_SIZE,
                    PANEL_X + 8,
                    PANEL_Y + PANEL_SIZE,
                    List.of(Component.translatable("desc.shared.errorInvalidFluid")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

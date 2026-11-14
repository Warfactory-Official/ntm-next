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

import com.hbm.blockentity.machine.dfc.DfcChamberBlockEntity;
import com.hbm.inventory.container.CoreMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class CoreScreen extends InfoContainerScreen<CoreMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/dfc/gui_core.png");

    private static final int GAUGE_W = 16, GAUGE_H = 52, GAUGE_Y = 17;
    private static final int FIELD_X = 8, TANK_A_X = 26, TANK_B_X = 134, HEAT_X = 152;

    public CoreScreen(CoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        DfcChamberBlockEntity be = menu.blockEntity();

        int field = be.getFieldScaled(GAUGE_H);
        if (field > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    FIELD_X,
                    GAUGE_Y + (GAUGE_H - field),
                    176,
                    GAUGE_H - field,
                    GAUGE_W,
                    field,
                    256,
                    256);
        }
        int heat = be.getHeatScaled(GAUGE_H);
        if (heat > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    HEAT_X,
                    GAUGE_Y + (GAUGE_H - heat),
                    192,
                    GAUGE_H - heat,
                    GAUGE_W,
                    heat,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK_A_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[0]);
        drawFluidBar(graphics, TANK_B_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[1]);

        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_A_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[0]);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_B_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[1]);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                FIELD_X,
                GAUGE_Y,
                GAUGE_W,
                GAUGE_H,
                List.of(Component.translatable("desc.gui.dfc.field", be.field)));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                HEAT_X,
                GAUGE_Y,
                GAUGE_W,
                GAUGE_H,
                List.of(Component.translatable("desc.gui.dfc.heat", be.heat)));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

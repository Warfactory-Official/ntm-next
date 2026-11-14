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

import com.hbm.blockentity.machine.oil.OilDrillBaseBlockEntity;
import com.hbm.inventory.container.OilWellMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class OilWellScreen extends InfoContainerScreen<OilWellMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/machine/gui_well.png");

    public OilWellScreen(OilWellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 184, 190);
        this.titleLabelY = 10;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 126;
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

        OilDrillBaseBlockEntity be = drill();
        var extra = be.extraTank();

        int i = (int) (be.power * 34L / be.getMaxPower());
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 56 - i, 184, 34 - i, 16, i, 256, 256);

        if (be.indicator != 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    50,
                    19,
                    184 + (be.indicator - 1) * 14,
                    34,
                    14,
                    14,
                    256,
                    256);
        }

        if (extra == null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 48, 44, 200, 0, 18, 34, 256, 256);
        } else {
            drawFluidBar(graphics, 54, 45, 6, 32, extra);
        }

        drawFluidBar(graphics, 76, 22, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 112, 22, 16, 52, be.tanks[1]);
        drawInfoPanel(graphics, 160, 21, 8, 8, 8);

        drawElectricityInfo(graphics, mouseX, mouseY, 8, 22, 16, 34, be.power, be.getMaxPower());
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 76, 22, 16, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 112, 22, 16, 52, be.tanks[1]);
        if (extra != null) drawFluidGaugeInfo(graphics, mouseX, mouseY, 54, 45, 6, 32, extra);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                160,
                21,
                8,
                8,
                resolveLines(
                        "desc.gui.upgrade",
                        "desc.gui.upgrade.speed",
                        "desc.gui.upgrade.power",
                        "desc.gui.upgrade.afterburner",
                        "desc.gui.upgrade.overdrive"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private OilDrillBaseBlockEntity drill() {
        return menu.blockEntity();
    }
}

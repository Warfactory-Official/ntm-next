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

import com.hbm.blockentity.machine.oil.PyroOvenBlockEntity;
import com.hbm.inventory.container.PyroOvenMenu;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class PyroOvenScreen extends InfoContainerScreen<PyroOvenMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_pyrooven.png");

    public PyroOvenScreen(PyroOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 70;
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

        PyroOvenBlockEntity be = menu.blockEntity();

        int i = (int) (be.power * 52 / PyroOvenBlockEntity.MAX_POWER);
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    152,
                    70 - i,
                    176,
                    64 - i,
                    16,
                    i,
                    256,
                    256);

        int p = (int) (be.progress * 27);
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 57, 47, 176, 0, p, 12, 256, 256);

        drawFluidBar(graphics, 8, 18, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 116, 18, 16, 52, be.tanks[1]);
        drawInfoPanel(graphics, leftPos + 108, topPos + 76, 8, 8, 8);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 18, 16, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 116, 18, 16, 52, be.tanks[1]);
        drawElectricityInfo(
                graphics, mouseX, mouseY, 152, 18, 16, 52, be.power, PyroOvenBlockEntity.MAX_POWER);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                108,
                76,
                8,
                8,
                108,
                76,
                upgradeInfo(be, UpgradeType.SPEED, UpgradeType.POWER));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

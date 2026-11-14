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

import com.hbm.blockentity.machine.fusion.FusionNeutronBlockEntity;
import com.hbm.inventory.container.FusionBreederMenu;
import com.hbm.util.NumberFormatter;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FusionBreederScreen extends InfoContainerScreen<FusionBreederMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/reactors/gui_fusion_breeder.png");

    private static final int TANK_IN_X = 26,
            TANK_OUT_X = 134,
            TANK_Y = 18,
            TANK_W = 16,
            TANK_H = 52;
    private static final int FLUX_X = 79, FLUX_Y = 23, FLUX_W = 18, FLUX_H = 18;
    private static final int BAR_X = 67, BAR_Y = 46, BAR_W = 42, BAR_H = 14;

    public FusionBreederScreen(FusionBreederMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 200);
        this.inventoryLabelX = 35;
        this.inventoryLabelY = this.imageHeight - 93;
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

        FusionNeutronBlockEntity be = menu.blockEntity();

        int p = (int) Math.ceil(be.progress * 42 / FusionNeutronBlockEntity.CAPACITY);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 67, 48, 176.0F, 0.0F, p, 10, 256, 256);
        }

        double gauge =
                1D - Math.pow(Math.E, -be.neutronEnergy * 10 / FusionNeutronBlockEntity.CAPACITY);
        SmoothGaugeElement.draw(graphics, 88, 32, gauge, 5, 2, 1, 0xA00000, 0x000000);

        drawFluidBar(graphics, TANK_IN_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidBar(graphics, TANK_OUT_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                FLUX_X,
                FLUX_Y,
                FLUX_W,
                FLUX_H,
                List.of(
                        Component.literal(
                                ChatFormatting.GREEN
                                        + "-> "
                                        + ChatFormatting.RESET
                                        + (int) Math.ceil(be.neutronEnergy)
                                        + " flux/t")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                BAR_X,
                BAR_Y,
                BAR_W,
                BAR_H,
                List.of(
                        Component.literal(
                                NumberFormatter.formatGrouped((int) Math.ceil(be.progress))
                                        + " / "
                                        + NumberFormatter.formatGrouped(
                                                (int) Math.ceil(FusionNeutronBlockEntity.CAPACITY))
                                        + " flux")));

        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_IN_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_OUT_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);
        super.extractLabels(graphics, mouseX, mouseY);
    }
}

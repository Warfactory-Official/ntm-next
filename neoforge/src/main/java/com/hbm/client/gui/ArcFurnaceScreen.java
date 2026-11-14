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

import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import com.hbm.inventory.container.ArcFurnaceMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ArcFurnaceLiquidModeLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ArcFurnaceScreen extends InfoContainerScreen<ArcFurnaceMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_arc_furnace.png");

    private static final int TOGGLE_X = 151, TOGGLE_Y = 17;
    private static final int PROGRESS_ICON_X = 7, PROGRESS_ICON_Y = 17;
    private static final int POWER_X = 8, POWER_Y = 36, POWER_W = 7, POWER_H = 70;
    private static final int PROGRESS_BAR_X = 17;
    private static final int STACK_X = 152, STACK_Y = 106, STACK_INFO_Y = 36;
    private static final MoltenFluidBar MELT =
            new MoltenFluidBar(TEXTURE, 208, 208, 16, POWER_H, POWER_H);

    public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
        inventoryLabelY = imageHeight - 96 + 2;
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
                256,
                256);

        ArcFurnaceBlockEntity be = furnace();

        if (be.liquidMode)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    TOGGLE_Y,
                    190,
                    18,
                    18,
                    18,
                    256,
                    256);
        if (be.isProgressing)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROGRESS_ICON_X,
                    PROGRESS_ICON_Y,
                    190,
                    0,
                    18,
                    18,
                    256,
                    256);

        int p = (int) (be.getPower() * POWER_H / be.getMaxPower());
        if (p > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - p),
                    176,
                    POWER_H - p,
                    7,
                    p,
                    256,
                    256);

        int o = (int) (be.progress * POWER_H);
        if (o > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROGRESS_BAR_X,
                    POWER_Y + (POWER_H - o),
                    183,
                    POWER_H - o,
                    7,
                    o,
                    256,
                    256);

        MELT.draw(graphics, be.liquids, ArcFurnaceBlockEntity.MAX_LIQUID, STACK_X, STACK_Y);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                STACK_X,
                STACK_INFO_Y,
                16,
                POWER_H,
                MoltenFluidBar.describe(be.liquids));
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.getPower(),
                be.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, 18, 18)) {
            PacketWire.sendToServer(ArcFurnaceLiquidModeLayout.encode(menu.containerId));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private ArcFurnaceBlockEntity furnace() {
        return menu.blockEntity();
    }
}

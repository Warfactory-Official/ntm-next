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

import com.hbm.blockentity.machine.MiningLaserBlockEntity;
import com.hbm.inventory.container.MiningLaserMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MiningLaserToggleLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class MiningLaserScreen extends InfoContainerScreen<MiningLaserMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_laser_miner.png");

    private static final int TOGGLE_X = 61, TOGGLE_Y = 17, TOGGLE_W = 18, TOGGLE_H = 18;
    private static final int POWER_X = 8, POWER_Y = 18, POWER_W = 16, POWER_H = 88;
    private static final int TANK_X = 35, TANK_Y = 72, TANK_W = 7, TANK_H = 52;
    private static final int PANEL_X = 87, PANEL_Y = 31, PANEL_W = 8, PANEL_H = 8;

    public MiningLaserScreen(MiningLaserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.titleLabelY = 4;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        MiningLaserBlockEntity be = laser();

        if (be.isOn) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    TOGGLE_Y,
                    200,
                    0,
                    TOGGLE_W,
                    TOGGLE_H,
                    256,
                    256);
        }

        int p = menu.getPowerScaled(POWER_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    106 - p,
                    176,
                    88 - p,
                    16,
                    p,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 8);

        String width = String.valueOf(be.getWidth());
        graphics.text(this.font, width, 43 - this.font.width(width) / 2, 26, 0xFFFFFFFF, false);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                MiningLaserBlockEntity.MAX_POWER);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y,
                PANEL_W,
                PANEL_H,
                141,
                55,
                lineArray("desc.gui.miningLaser.upgrades"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick(
                        (int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            PacketWire.sendToServer(MiningLaserToggleLayout.encode(menu.containerId));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private MiningLaserBlockEntity laser() {
        return menu.blockEntity();
    }
}

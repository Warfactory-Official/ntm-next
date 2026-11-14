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

import com.hbm.blockentity.machine.oil.GasFlareBlockEntity;
import com.hbm.inventory.container.GasFlareMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.GasFlareBurnModeLayout;
import com.hbm.packet.layout.GasFlareValveLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GasFlareScreen extends InfoContainerScreen<GasFlareMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/generators/gui_flare_stack.png");

    private static final int TANK_X = 35, TANK_Y = 17, TANK_W = 16, TANK_H = 52;
    private static final int POWER_X = 143, POWER_Y = 17, POWER_W = 16, POWER_H = 52;
    private static final int VALVE_X = 79, VALVE_Y = 15, VALVE_W = 35, VALVE_H = 10;
    private static final int DIAL_X = 79, DIAL_Y = 49, DIAL_W = 35, DIAL_H = 14;
    private static final int VALVE_HIT_Y = 16, DIAL_HIT_Y = 50;
    private static final int VALVE_BTN_X = 89, VALVE_BTN_Y = 16, VALVE_BTN_W = 16, VALVE_BTN_H = 10;
    private static final int DIAL_BTN_X = 89, DIAL_BTN_Y = 50, DIAL_BTN_W = 16, DIAL_BTN_H = 14;

    public GasFlareScreen(GasFlareMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 203);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected boolean drawTitle() {
        return false;
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

        GasFlareBlockEntity be = flare();

        int power = (int) (be.power * POWER_H / GasFlareBlockEntity.MAX_POWER);
        if (power > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - power),
                    176,
                    94 - power,
                    POWER_W,
                    power,
                    256,
                    256);
        }

        if (be.valve)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    VALVE_X,
                    VALVE_Y,
                    176,
                    0,
                    VALVE_W,
                    VALVE_H,
                    256,
                    256);
        if (be.ignition)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    DIAL_X,
                    DIAL_Y,
                    176,
                    10,
                    DIAL_W,
                    DIAL_H,
                    256,
                    256);

        if (be.valve
                && be.ignition
                && be.tank.getFill() > 0
                && NTMFluidProperties.hasTrait(be.tank.getTankType(), FlammableTrait.class)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 88, 29, 176, 24, 18, 18, 256, 256);
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                GasFlareBlockEntity.MAX_POWER);

        if (checkClick(mouseX, mouseY, VALVE_X, VALVE_HIT_Y, VALVE_W, VALVE_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    VALVE_X,
                    VALVE_HIT_Y,
                    VALVE_W,
                    VALVE_H,
                    List.of(Component.translatable("flare.valve")));
        }
        if (checkClick(mouseX, mouseY, DIAL_X, DIAL_HIT_Y, DIAL_W, DIAL_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    DIAL_X,
                    DIAL_HIT_Y,
                    DIAL_W,
                    DIAL_H,
                    List.of(Component.translatable("flare.ignition")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, VALVE_BTN_X, VALVE_BTN_Y, VALVE_BTN_W, VALVE_BTN_H)) {
                PacketWire.sendToServer(GasFlareValveLayout.encode(menu.containerId));
                playClick();
                return true;
            }
            if (checkClick(mx, my, DIAL_BTN_X, DIAL_BTN_Y, DIAL_BTN_W, DIAL_BTN_H)) {
                PacketWire.sendToServer(GasFlareBurnModeLayout.encode(menu.containerId));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private GasFlareBlockEntity flare() {
        return menu.blockEntity();
    }
}

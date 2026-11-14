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

import com.hbm.blockentity.machine.HeaterOilburnerBlockEntity;
import com.hbm.inventory.container.OilburnerMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.OilburnerToggleLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class OilburnerScreen extends InfoContainerScreen<OilburnerMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_oilburner.png");

    private static final int TANK_X = 44, TANK_Y = 17, TANK_W = 16, TANK_H = 52;
    private static final int HEAT_X = 116, HEAT_Y = 17, HEAT_W = 16, HEAT_H = 52;
    private static final int FLAME_X = 79, FLAME_Y = 34, FLAME_W = 18, FLAME_H = 18;
    private static final int ON_X = 70, ON_Y = 54, ON_W = 35, ON_H = 14;
    private static final int TOGGLE_X = 80, TOGGLE_Y = 54, TOGGLE_W = 16, TOGGLE_H = 14;

    public OilburnerScreen(OilburnerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 203);
        this.titleLabelY = 6;
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

        HeaterOilburnerBlockEntity be = burner();

        int i = be.heatEnergy * HEAT_H / HeaterOilburnerBlockEntity.MAX_HEAT_ENERGY;
        if (i > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    HEAT_X,
                    HEAT_Y + (HEAT_H - i),
                    194,
                    HEAT_H - i,
                    HEAT_W,
                    i,
                    256,
                    256);
        }

        Fluid type = be.tank.getTankType();
        FlammableTrait trait = NTMFluidProperties.getTrait(type, FlammableTrait.class);

        if (be.isOn) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ON_X,
                    ON_Y,
                    210,
                    0,
                    ON_W,
                    ON_H,
                    256,
                    256);
            if (trait != null && be.tank.getFill() > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        FLAME_X,
                        FLAME_Y,
                        176,
                        0,
                        FLAME_W,
                        FLAME_H,
                        256,
                        256);
            }
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        if (checkClick(mouseX, mouseY, HEAT_X, HEAT_Y, HEAT_W, HEAT_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    HEAT_X,
                    HEAT_Y,
                    HEAT_W,
                    HEAT_H,
                    List.of(
                            Component.literal(
                                    String.format(Locale.ROOT, "%,d", be.heatEnergy)
                                            + " / "
                                            + String.format(
                                                    Locale.ROOT,
                                                    "%,d",
                                                    HeaterOilburnerBlockEntity.MAX_HEAT_ENERGY)
                                            + " TU")));
        }

        if (trait != null && checkClick(mouseX, mouseY, FLAME_X, FLAME_Y, FLAME_W, FLAME_H)) {
            int heat = (int) (trait.heatPerBucket() * be.setting / 1000L);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    FLAME_X,
                    FLAME_Y,
                    FLAME_W,
                    FLAME_H,
                    List.of(
                            Component.literal(be.setting + " mB/t"),
                            Component.literal(heat + " TU/t")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick(
                        (int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            PacketWire.sendToServer(OilburnerToggleLayout.encode(menu.containerId));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private HeaterOilburnerBlockEntity burner() {
        return menu.blockEntity();
    }
}

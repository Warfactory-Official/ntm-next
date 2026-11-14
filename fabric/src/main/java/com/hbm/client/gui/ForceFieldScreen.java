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

import com.hbm.blockentity.machine.RepulsionFieldBlockEntity;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.ForceFieldMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ForceFieldToggleLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ForceFieldScreen extends InfoContainerScreen<ForceFieldMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/gui_field.png");

    private static final int BAR_H = 52;
    private static final int BAR_BOTTOM = 69;
    private static final int POWER_X = 8, HEALTH_X = 62, BAR_W = 16;
    private static final int TOGGLE_X = 142, TOGGLE_Y = 34, TOGGLE_W = 18, TOGGLE_H = 18;

    public ForceFieldScreen(ForceFieldMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
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

        RepulsionFieldBlockEntity field = field();

        int p = menu.getPowerScaled(BAR_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    BAR_BOTTOM - p,
                    176,
                    BAR_H - p,
                    BAR_W,
                    p,
                    256,
                    256);
        }

        int h = field.healthBarPixels(BAR_H);
        if (h > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    HEALTH_X,
                    BAR_BOTTOM - h,
                    192,
                    BAR_H - h,
                    BAR_W,
                    h,
                    256,
                    256);
        }

        if (field.requestedOn) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    TOGGLE_Y,
                    176,
                    52,
                    TOGGLE_W,
                    TOGGLE_H,
                    256,
                    256);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                BAR_BOTTOM - BAR_H,
                BAR_W,
                BAR_H,
                menu.getPower(),
                MachineData.FORCE_FIELD_MAX_POWER.get());
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                HEALTH_X,
                BAR_BOTTOM - BAR_H,
                BAR_W,
                BAR_H,
                List.of(
                        Component.literal(
                                field.shieldHealth + " / " + field.shieldCapacity + "HP")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick(
                        (int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            PacketWire.sendToServer(ForceFieldToggleLayout.encode(menu.containerId));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private RepulsionFieldBlockEntity field() {
        return menu.blockEntity();
    }
}

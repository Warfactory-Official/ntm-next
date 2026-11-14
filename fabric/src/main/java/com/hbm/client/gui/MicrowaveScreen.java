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

import com.hbm.blockentity.machine.MicrowaveBlockEntity;
import com.hbm.inventory.container.MicrowaveMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MicrowaveSpeedLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class MicrowaveScreen extends InfoContainerScreen<MicrowaveMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_microwave.png");

    private static final int BTN_X = 43, BTN_UP_Y = 25, BTN_DOWN_Y = 43, BTN_W = 18, BTN_H = 18;

    public MicrowaveScreen(MicrowaveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
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

        MicrowaveBlockEntity be = menu.blockEntity();

        int i = (int) (be.energy * 34L / MicrowaveBlockEntity.ENERGY_CAPACITY);
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 51 - i, 176, 34 - i, 16, i, 256, 256);

        int j = Math.min(be.progress * 23 / MicrowaveBlockEntity.PROGRESS_REQUIRED, 22);
        if (j > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 104, 34, 192, 0, j, 16, 256, 256);

        int k = be.speed * 34 / MicrowaveBlockEntity.SPEED_LIMIT;
        if (k > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 62, 60 - k, 214, 34 - k, 4, k, 256, 256);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                8,
                17,
                16,
                34,
                be.energy,
                MicrowaveBlockEntity.ENERGY_CAPACITY);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x(), my = (int) event.y();
            int button = -1;
            if (checkClick(mx, my, BTN_X, BTN_UP_Y, BTN_W, BTN_H)) button = 0;
            else if (checkClick(mx, my, BTN_X, BTN_DOWN_Y, BTN_W, BTN_H)) button = 1;
            if (button >= 0) {
                PacketWire.sendToServer(MicrowaveSpeedLayout.encode(menu.containerId, button));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

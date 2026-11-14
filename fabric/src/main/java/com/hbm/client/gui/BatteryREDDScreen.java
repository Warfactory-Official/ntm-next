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

import com.hbm.blockentity.machine.storage.ReddBlockEntity;
import com.hbm.inventory.container.BatteryREDDMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MachineBatteryPriorityLayout;
import com.hbm.packet.layout.MachineBatteryRedHighLayout;
import com.hbm.packet.layout.MachineBatteryRedLowLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class BatteryREDDScreen extends InfoContainerScreen<BatteryREDDMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_battery_redd.png");

    private static final int LOW_X = 133, LOW_Y = 16, MODE_W = 18, MODE_H = 18;
    private static final int HIGH_X = 133, HIGH_Y = 52;
    private static final int PRIO_X = 152, PRIO_Y = 35, PRIO_W = 16, PRIO_H = 16;

    public BatteryREDDScreen(BatteryREDDMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 181);
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

        ReddBlockEntity battery = menu.blockEntity();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                LOW_X,
                LOW_Y,
                176,
                52 + menu.getRedLow() * MODE_H,
                MODE_W,
                MODE_H,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                HIGH_X,
                HIGH_Y,
                176,
                52 + menu.getRedHigh() * MODE_H,
                MODE_W,
                MODE_H,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                PRIO_X,
                PRIO_Y,
                194,
                52 + (menu.getPriorityOrdinal() - 1) * PRIO_H,
                PRIO_W,
                PRIO_H,
                256,
                256);

        String charge = String.format(Locale.ROOT, "%,d", battery.getExactStock()) + " HE";
        drawHalfScaleRight(graphics, Component.literal(charge), 45);

        int sign = battery.getExactDelta().signum();
        String rate = String.format(Locale.ROOT, "%,d", battery.getExactDelta()) + " HE/s";
        ChatFormatting colour =
                sign > 0
                        ? ChatFormatting.GREEN
                        : sign < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
        drawHalfScaleRight(
                graphics, Component.literal(sign < 0 ? rate : "+" + rate).withStyle(colour), 65);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawHalfScaleRight(GuiGraphicsExtractor graphics, Component line, int y) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        graphics.text(font, line, 242 - font.width(line), y, 0xFF00FF00, false);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x();
            int my = (int) event.y();
            boolean redLow = checkClick(mx, my, LOW_X, LOW_Y, MODE_W, MODE_H);
            boolean redHigh = checkClick(mx, my, HIGH_X, HIGH_Y, MODE_W, MODE_H);
            boolean priority = checkClick(mx, my, PRIO_X, PRIO_Y, PRIO_W, PRIO_H);
            if (redLow || redHigh || priority) {
                if (redLow)
                    PacketWire.sendToServer(MachineBatteryRedLowLayout.encode(menu.containerId));
                if (redHigh)
                    PacketWire.sendToServer(MachineBatteryRedHighLayout.encode(menu.containerId));
                if (priority)
                    PacketWire.sendToServer(MachineBatteryPriorityLayout.encode(menu.containerId));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

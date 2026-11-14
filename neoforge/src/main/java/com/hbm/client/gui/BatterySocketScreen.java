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

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.inventory.container.BatterySocketMenu;
import com.hbm.items.machine.BatteryCreativeItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.BatterySocketPriorityLayout;
import com.hbm.packet.layout.BatterySocketRedHighLayout;
import com.hbm.packet.layout.BatterySocketRedLowLayout;
import com.hbm.util.NumberFormatter;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class BatterySocketScreen extends InfoContainerScreen<BatterySocketMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_battery_socket.png");

    private static final int CHARGE_X = 62, CHARGE_Y = 17, CHARGE_W = 34, CHARGE_H = 52;
    private static final int LOW_X = 106, LOW_Y = 16, MODE_W = 18, MODE_H = 18;
    private static final int HIGH_X = 106, HIGH_Y = 52;
    private static final int PRIO_X = 125, PRIO_Y = 35, PRIO_W = 16, PRIO_H = 16;

    private final long[] chargeLog = new long[20];
    private long prevCharge;
    private long delta;
    private boolean logPrimed;

    public BatterySocketScreen(BatterySocketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 181);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        long power = currentCharge();
        if (!logPrimed) {
            Arrays.fill(chargeLog, power);
            prevCharge = power;
            logPrimed = true;
        }
        long avg = (power + prevCharge) / 2;
        delta = avg - chargeLog[0];
        System.arraycopy(chargeLog, 1, chargeLog, 0, chargeLog.length - 1);
        chargeLog[chargeLog.length - 1] = avg;
        prevCharge = power;
    }

    private long currentCharge() {
        ItemStack battery = menu.getBattery();
        return battery.getItem() instanceof EnergyItem item ? item.getCharge(battery) : 0L;
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

        ItemStack battery = menu.getBattery();
        long charge = 0L, max = 0L;
        if (battery.getItem() instanceof BatteryCreativeItem) {
            charge = 1L;
            max = 1L;
        } else if (battery.getItem() instanceof EnergyItem item) {
            charge = item.getCharge(battery);
            max = item.getMaxCharge(battery);
        }
        if (max > 0L) {
            if (charge > Long.MAX_VALUE / 100L) {
                charge /= 100L;
                max /= 100L;
            }
            int p = (int) (charge * CHARGE_H / max);
            if (p > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        CHARGE_X,
                        CHARGE_Y + (CHARGE_H - p),
                        176,
                        CHARGE_H - p,
                        CHARGE_W,
                        p,
                        256,
                        256);
            }
        }

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

        if (battery.getItem() instanceof EnergyItem item) {
            String chargeLine =
                    NumberFormatter.formatCompact(item.getCharge(battery))
                            + "/"
                            + NumberFormatter.formatCompact(item.getMaxCharge(battery))
                            + "HE";
            String rate = NumberFormatter.formatCompact(Math.abs(delta)) + "HE/s";
            ChatFormatting color =
                    delta > 0
                            ? ChatFormatting.GREEN
                            : delta < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
            Component rateLine = Component.literal((delta < 0 ? "-" : "+") + rate).withStyle(color);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    CHARGE_X,
                    CHARGE_Y,
                    CHARGE_W,
                    CHARGE_H,
                    List.of(Component.literal(chargeLine), rateLine));
        }
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PRIO_X,
                PRIO_Y,
                PRIO_W,
                PRIO_H,
                priorityInfo(menu.getPriorityOrdinal()));

        super.extractLabels(graphics, mouseX, mouseY);
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
                    PacketWire.sendToServer(BatterySocketRedLowLayout.encode(menu.containerId));
                if (redHigh)
                    PacketWire.sendToServer(BatterySocketRedHighLayout.encode(menu.containerId));
                if (priority)
                    PacketWire.sendToServer(BatterySocketPriorityLayout.encode(menu.containerId));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

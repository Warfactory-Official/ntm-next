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

import com.hbm.blockentity.machine.SoyuzLauncherBlockEntity;
import com.hbm.inventory.container.SoyuzLauncherMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.SoyuzLauncherModeLayout;
import com.hbm.packet.layout.SoyuzLauncherStartLayout;
import com.hbm.util.RegistryUtil;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

@Deprecated
public class SoyuzLauncherScreen extends InfoContainerScreen<SoyuzLauncherMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/machine/gui_soyuz.png");

    public SoyuzLauncherScreen(SoyuzLauncherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 244);
        titleLabelY = 4;
        inventoryLabelX = 17;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    private SoyuzLauncherBlockEntity launcher() {
        return menu.blockEntity();
    }

    @Override
    protected int titleColor() {
        return 0xFFFFFFFF;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SoyuzLauncherBlockEntity launcher = launcher();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);

        int power = (int) launcher.getPowerScaled(52);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                134,
                96 - power,
                194,
                52 - power,
                16,
                power,
                256,
                256);
        state(graphics, 97, 79, launcher.hasRocket() ? 2 : 1);
        state(graphics, 79, 79, launcher.designator());
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                97 - launcher.mode * 18,
                52,
                228 - launcher.mode * 18,
                26,
                18,
                18,
                256,
                256);
        state(graphics, 79, 25, launcher.orbital());
        state(graphics, 97, 25, launcher.satellite());
        if (launcher.starting)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 88, 97, 210, 44, 18, 18, 256, 256);

        pip(graphics, 157, 31, launcher.hasFuel());
        pip(graphics, 175, 31, launcher.hasOxy());
        pip(graphics, 139, 31, launcher.hasPower());
        drawFluidBar(graphics, 152, 44, 16, 52, launcher.fuelTank);
        drawFluidBar(graphics, 170, 44, 16, 52, launcher.oxidizerTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 152, 44, 16, 52, launcher.fuelTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 170, 44, 16, 52, launcher.oxidizerTank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                134,
                44,
                16,
                52,
                launcher.power,
                SoyuzLauncherBlockEntity.MAX_POWER);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                -16,
                53,
                16,
                16,
                -8,
                69,
                lineArray("desc.gui.soyuz.desc"));
        drawCustomInfoStat(
                graphics, mouseX, mouseY, 79, 52, 18, 18, lineArray("desc.gui.soyuz.cargo"));
        drawCustomInfoStat(
                graphics, mouseX, mouseY, 97, 52, 18, 18, lineArray("desc.gui.soyuz.satellite"));
        drawInfoPanel(graphics, -16, 53, 16, 16, 2);

        super.extractLabels(graphics, mouseX, mouseY);
        clock(graphics, launcher.countdown);
    }

    private static void state(GuiGraphicsExtractor graphics, int x, int y, int value) {
        if (value > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    x,
                    y,
                    210 + (value - 1) * 18,
                    8,
                    18,
                    18,
                    256,
                    256);
    }

    private static void pip(GuiGraphicsExtractor graphics, int x, int y, boolean ready) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, ready ? 210 : 216, 0, 6, 8, 256, 256);
    }

    private void clock(GuiGraphicsExtractor graphics, int countdown) {
        String seconds = String.valueOf(countdown / SharedConstants.TICKS_PER_SECOND);
        String centiseconds = String.valueOf(countdown % SharedConstants.TICKS_PER_SECOND * 5);
        if (seconds.length() == 1) seconds = "0" + seconds;
        if (centiseconds.length() == 1) centiseconds += "0";
        graphics.text(
                font, Component.literal(seconds + ":" + centiseconds), 85, 121, 0xFFFF0000, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (clickMode(event, 97, 52, SoyuzLauncherBlockEntity.MODE_SATELLITE)) return true;
        if (clickMode(event, 79, 52, SoyuzLauncherBlockEntity.MODE_CARGO)) return true;
        if (clickStart(event, 88, 97)) return true;
        return handled;
    }

    private boolean clickMode(MouseButtonEvent event, int x, int y, int mode) {
        if (!checkClick((int) event.x(), (int) event.y(), x, y, 18, 18)) return false;
        playClick();
        PacketWire.sendToServer(SoyuzLauncherModeLayout.encode(menu.containerId, mode));
        return true;
    }

    private boolean clickStart(MouseButtonEvent event, int x, int y) {
        if (!checkClick((int) event.x(), (int) event.y(), x, y, 18, 18)) return false;
        playClick();
        PacketWire.sendToServer(SoyuzLauncherStartLayout.encode(menu.containerId));
        return true;
    }
}

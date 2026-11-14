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

import com.hbm.blockentity.machine.LaunchpadSoyuzBlockEntity;
import com.hbm.inventory.container.LaunchpadSoyuzMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.SoyuzPadCargoLayout;
import com.hbm.packet.layout.SoyuzPadLaunchLayout;
import com.hbm.util.RegistryUtil;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class LaunchpadSoyuzScreen extends InfoContainerScreen<LaunchpadSoyuzMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_launchpad_soyuz.png");

    public LaunchpadSoyuzScreen(LaunchpadSoyuzMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 244);
        titleLabelY = 4;
        inventoryLabelX = 17;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    private LaunchpadSoyuzBlockEntity pad() {
        return menu.blockEntity();
    }

    @Override
    protected int titleColor() {
        return 0xFFFFFFFF;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        LaunchpadSoyuzBlockEntity pad = pad();
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

        int power = (int) (pad.power * 52 / LaunchpadSoyuzBlockEntity.MAX_POWER);
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
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                pad.cargoMode ? 79 : 97,
                52,
                pad.cargoMode ? 210 : 228,
                26,
                18,
                18,
                256,
                256);
        pip(graphics, 157, 31, pad.hasJetFuel());
        pip(graphics, 175, 31, pad.hasOxidizer());
        pip(graphics, 139, 31, pad.power >= LaunchpadSoyuzBlockEntity.CONSUMPTION);

        int orbital = pad.orbital();
        if (orbital > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    79,
                    25,
                    210 + (orbital - 1) * 18,
                    8,
                    18,
                    18,
                    256,
                    256);
        if (pad.soyuzStatus == LaunchpadSoyuzBlockEntity.SoyuzStatus.LAUNCHING)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 88, 97, 210, 44, 18, 18, 256, 256);

        drawFluidBar(graphics, 152, 44, 16, 52, pad.fuelTank);
        drawFluidBar(graphics, 170, 44, 16, 52, pad.oxidizerTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 152, 44, 16, 52, pad.fuelTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 170, 44, 16, 52, pad.oxidizerTank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                134,
                44,
                16,
                52,
                pad.power,
                LaunchpadSoyuzBlockEntity.MAX_POWER);

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
        status(graphics, pad);
    }

    private static void pip(GuiGraphicsExtractor graphics, int x, int y, boolean ready) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, ready ? 210 : 216, 0, 6, 8, 256, 256);
    }

    private void status(GuiGraphicsExtractor graphics, LaunchpadSoyuzBlockEntity pad) {
        if (pad.soyuzStatus == LaunchpadSoyuzBlockEntity.SoyuzStatus.LAUNCHING) {
            int countdown = pad.countdown;
            String seconds = String.valueOf(countdown / SharedConstants.TICKS_PER_SECOND);
            String centiseconds = String.valueOf(countdown % SharedConstants.TICKS_PER_SECOND * 5);
            if (seconds.length() == 1) seconds = "0" + seconds;
            if (centiseconds.length() == 1) centiseconds += "0";
            graphics.text(
                    font,
                    Component.literal(seconds + ":" + centiseconds),
                    85,
                    121,
                    0xFFFF0000,
                    false);
            return;
        }
        String key;
        int color;
        switch (pad.soyuzStatus) {
            case ABSENT -> {
                key = "desc.gui.soyuz.idle";
                color = 0xFF0000;
            }
            case LOADING -> {
                key = "desc.gui.soyuz.loading";
                color = 0xFF8000;
            }
            case FUELING -> {
                key = "desc.gui.soyuz.fueling";
                color = 0xFFFF00;
            }
            case READY -> {
                key = "desc.gui.soyuz.ready";
                color = 0x00FF00;
            }
            default -> throw new IllegalStateException();
        }
        Component label = Component.translatable(key);
        float scale = Math.min(1F, 22F / font.width(label));
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(
                font,
                label,
                (int) (97 / scale - font.width(label) / 2F),
                (int) (125 / scale - font.lineHeight / 2F),
                0xFF000000 | color,
                false);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (clickCargo(event, 79, 52, true)) return true;
        if (clickCargo(event, 97, 52, false)) return true;
        if (pad().soyuzStatus == LaunchpadSoyuzBlockEntity.SoyuzStatus.READY
                && clickLaunch(event, 88, 97)) return true;
        return handled;
    }

    private boolean clickCargo(MouseButtonEvent event, int x, int y, boolean cargo) {
        if (!checkClick((int) event.x(), (int) event.y(), x, y, 18, 18)) return false;
        playClick();
        PacketWire.sendToServer(SoyuzPadCargoLayout.encode(menu.containerId, cargo));
        return true;
    }

    private boolean clickLaunch(MouseButtonEvent event, int x, int y) {
        if (!checkClick((int) event.x(), (int) event.y(), x, y, 18, 18)) return false;
        playClick();
        PacketWire.sendToServer(SoyuzPadLaunchLayout.encode(menu.containerId));
        return true;
    }
}

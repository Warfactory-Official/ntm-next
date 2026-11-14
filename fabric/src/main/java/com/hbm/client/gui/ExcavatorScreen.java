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

import com.hbm.blockentity.machine.ExcavatorBlockEntity;
import com.hbm.inventory.container.ExcavatorMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ExcavatorSettingsLayout;
import com.hbm.sound.ModSounds;
import com.hbm.util.GameTime;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ExcavatorScreen extends InfoContainerScreen<ExcavatorMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_mining_drill.png");

    private static final int TOGGLE_Y = 42, TOGGLE_W = 20, TOGGLE_H = 40;
    private static final int LAMP_Y = 5, LAMP_W = 10, LAMP_H = 10;
    private static final int POWER_X = 220, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int TANK_X = 202, TANK_Y = 18, TANK_W = 16, TANK_H = 52;

    private static final Toggle[] TOGGLES = {
        new Toggle(6, "drill", "excavator.drill"),
        new Toggle(30, "crusher", "excavator.crusher"),
        new Toggle(54, "walling", "excavator.walling"),
        new Toggle(78, "veinminer", "excavator.veinminer"),
        new Toggle(102, "silktouch", "excavator.silktouch"),
    };

    public ExcavatorScreen(ExcavatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 242, 204);
        inventoryLabelX = 8 + 33;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, 242, 96, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, 33, 104, 33.0F, 104.0F, 176, 100, 256, 256);

        ExcavatorBlockEntity be = excavator();
        boolean blink = GameTime.millis() % 1000L < 500L;

        int p = menu.getPowerScaled(POWER_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    70 - p,
                    229.0F,
                    156.0F - p,
                    16,
                    p,
                    256,
                    256);
        }

        if (menu.getPower() > be.getPowerConsumption()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 224, 4, 239.0F, 156.0F, 9, 12, 256, 256);
        }

        if (be.getInstalledDrill() == null && blink) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    171,
                    74,
                    209.0F,
                    154.0F,
                    18,
                    18,
                    256,
                    256);
        }

        boolean powered =
                be.getInstalledDrill() != null && menu.getPower() >= be.getPowerConsumption();
        drawToggle(graphics, TOGGLES[0], be.enableDrill, powered, blink);
        drawToggle(graphics, TOGGLES[1], be.enableCrusher, true, blink);
        drawToggle(graphics, TOGGLES[2], be.enableWalling, true, blink);
        drawToggle(graphics, TOGGLES[3], be.enableVeinMiner, be.canVeinMine(), blink);
        drawToggle(graphics, TOGGLES[4], be.enableSilkTouch, be.canSilkTouch(), blink);

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        for (Toggle toggle : TOGGLES) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    toggle.x,
                    TOGGLE_Y,
                    TOGGLE_W,
                    TOGGLE_H,
                    List.of(Component.translatable(toggle.key)));
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                ExcavatorBlockEntity.MAX_POWER);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawToggle(
            GuiGraphicsExtractor graphics,
            Toggle toggle,
            boolean on,
            boolean ready,
            boolean blink) {
        if (!on) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                toggle.x,
                TOGGLE_Y,
                209.0F,
                114.0F,
                TOGGLE_W,
                TOGGLE_H,
                256,
                256);
        if (ready) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    toggle.x + 5,
                    LAMP_Y,
                    209.0F,
                    104.0F,
                    LAMP_W,
                    LAMP_H,
                    256,
                    256);
        } else if (blink) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    toggle.x + 5,
                    LAMP_Y,
                    219.0F,
                    104.0F,
                    LAMP_W,
                    LAMP_H,
                    256,
                    256);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();

        for (Toggle toggle : TOGGLES) {
            if (!checkClick(x, y, toggle.x, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) continue;

            Minecraft.getInstance()
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(ModSounds.LEVER_LARGE.get(), 1.0F));
            String key = toggle.packetKey;
            PacketWire.sendToServer(
                    ExcavatorSettingsLayout.encode(
                            menu.containerId,
                            key.equals("drill"),
                            key.equals("crusher"),
                            key.equals("walling"),
                            key.equals("veinminer"),
                            key.equals("silktouch")));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private ExcavatorBlockEntity excavator() {
        return menu.blockEntity();
    }

    private record Toggle(int x, String packetKey, String key) {}
}

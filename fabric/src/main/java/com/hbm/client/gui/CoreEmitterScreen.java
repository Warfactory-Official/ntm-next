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

import com.hbm.blockentity.machine.dfc.DfcEmitterBlockEntity;
import com.hbm.inventory.container.CoreEmitterMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.CoreEmitterToggleLayout;
import com.hbm.packet.layout.CoreEmitterWattsLayout;
import com.hbm.util.NumberFormatter;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class CoreEmitterScreen extends InfoContainerScreen<CoreEmitterMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/dfc/gui_emitter.png");

    private static final int GAUGE_W = 16, GAUGE_H = 52, GAUGE_Y = 17;
    private static final int TANK_X = 8, POWER_X = 26;
    private static final int SET_X = 97, TOGGLE_X = 133, BUTTON_Y = 52, BUTTON_SIZE = 18;
    private static final int SLIDER_X = 53, SLIDER_Y = 45, SLIDER_W = 34;
    private static final int FIELD_BOX_X = 53, FIELD_BOX_Y = 53;

    private EditBox field;

    public CoreEmitterScreen(CoreEmitterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(font, leftPos + 57, topPos + 57, 29, 12, Component.empty());
        field.setTextColor(-1);
        field.setTextColorUneditable(-1);
        field.setBordered(false);
        field.setMaxLength(3);
        field.setValue(String.valueOf(menu.blockEntity().watts));
        addRenderableWidget(field);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x();
            int my = (int) event.y();
            if (checkClick(mx, my, SET_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                Integer watts = parseWatts();
                if (watts != null) {
                    PacketWire.sendToServer(CoreEmitterWattsLayout.encode(menu.containerId, watts));
                    playClick();
                    return true;
                }
            }
            if (checkClick(mx, my, TOGGLE_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                PacketWire.sendToServer(CoreEmitterToggleLayout.encode(menu.containerId));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private @Nullable Integer parseWatts() {
        String text = field.getValue();
        if (text.isEmpty() || !text.chars().allMatch(Character::isDigit)) return null;
        int watts = Math.clamp(Integer.parseInt(text), 1, 100);
        field.setValue(String.valueOf(watts));
        return watts;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        if (field.isFocused()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + FIELD_BOX_X,
                    topPos + FIELD_BOX_Y,
                    210,
                    4,
                    34,
                    16,
                    256,
                    256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        DfcEmitterBlockEntity be = menu.blockEntity();

        if (be.isOn) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    BUTTON_Y,
                    192,
                    0,
                    BUTTON_SIZE,
                    BUTTON_SIZE,
                    256,
                    256);
        }
        int slider = be.watts * SLIDER_W / 100;
        if (slider > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    SLIDER_X,
                    SLIDER_Y,
                    210,
                    0,
                    slider,
                    4,
                    256,
                    256);
        }

        int power = (int) be.getPowerScaled(GAUGE_H);
        if (power > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    GAUGE_Y + (GAUGE_H - power),
                    176,
                    GAUGE_H - power,
                    GAUGE_W,
                    power,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tank);

        graphics.text(
                font,
                Component.translatable(
                        "desc.gui.coreEmitter.output", NumberFormatter.formatCompact(be.prev)),
                50,
                30,
                0xFFFF7F7F,
                false);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                GAUGE_Y,
                GAUGE_W,
                GAUGE_H,
                be.power,
                be.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

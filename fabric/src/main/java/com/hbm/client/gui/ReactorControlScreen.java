/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.blockentity.machine.ControlCurve;
import com.hbm.blockentity.machine.reactor.ReactorControlBlockEntity;
import com.hbm.inventory.container.ReactorControlMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ReactorControlCurveLayout;
import com.hbm.packet.layout.ReactorControlFunctionLayout;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ReactorControlScreen extends InfoContainerScreen<ReactorControlMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/gui_reactor_control.png");
    private static final int GREEN = 0xFF08FF00;

    private final NumberDisplay[] displays = {
        new NumberDisplay(6, 20, 0x08FF00).setDigitLength(3),
        new NumberDisplay(66, 20, 0x08FF00).setDigitLength(4),
        new NumberDisplay(126, 20, 0x08FF00).setDigitLength(3)
    };
    private final EditBox[] fields = new EditBox[4];
    private final float[] curvePoints = new float[80];

    public ReactorControlScreen(ReactorControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    private ReactorControlBlockEntity control() {
        return menu.blockEntity();
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 4; i++) {
            EditBox field =
                    new EditBox(
                            font,
                            leftPos + 35 + 30 * (i % 2),
                            topPos + (i < 2 ? 38 : 49),
                            26,
                            7,
                            Component.empty());
            field.setTextColor(GREEN);
            field.setTextColorUneditable(-1);
            field.setBordered(false);
            field.setMaxLength(i < 2 ? 3 : 4);
            fields[i] = addRenderableWidget(field);
        }
        var control = control();
        fields[0].setValue(String.valueOf((int) control.upperRodPercent));
        fields[1].setValue(String.valueOf((int) control.lowerRodPercent));
        fields[2].setValue(String.valueOf((int) control.upperHeat / 50));
        fields[3].setValue(String.valueOf((int) control.lowerHeat / 50));
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
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var control = control();
        for (int i = 0; i < 40; i++) {
            curvePoints[i * 2] = 128 + i;
            double level =
                    control.curve.level(
                            i * 1250,
                            control.lowerHeat,
                            control.upperHeat,
                            control.lowerRodPercent,
                            control.upperRodPercent);
            curvePoints[i * 2 + 1] = (float) (39 + Mth.clamp(level / 100 * 28, 0, 28));
        }
        GuiLineStrip.draw(graphics, curvePoints, GREEN);
        displays[0].draw(graphics, control.displayRodPercent(), true);
        displays[1].draw(graphics, control.displayFlux(), true);
        displays[2].draw(graphics, control.displayCelsius(), true);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        int x = (int) event.x(), y = (int) event.y();

        if (checkClick(x, y, 33, 59, 58, 10)) {
            playClick();
            double[] vals = new double[4];
            for (int k = 0; k < 4; k++) {
                int clamp = k < 2 ? 100 : 1000;
                int mod = k < 2 ? 1 : 50;
                String text = fields[k].getValue();
                if (!text.isEmpty() && text.chars().allMatch(c -> c >= '0' && c <= '9')) {
                    int j = (int) Mth.clamp(Double.parseDouble(text), 0, clamp);
                    fields[k].setValue(String.valueOf(j));
                    vals[k] = j * mod;
                } else {
                    fields[k].setValue("0");
                }
            }
            PacketWire.sendToServer(
                    ReactorControlCurveLayout.encode(
                            menu.containerId, vals[1], vals[0], vals[3], vals[2]));
            return true;
        }

        for (ControlCurve curve : ControlCurve.VALUES) {
            if (checkClick(x, y, 7, 37 + curve.ordinal() * 11, 22, 10)) {
                playClick();
                PacketWire.sendToServer(
                        ReactorControlFunctionLayout.encode(menu.containerId, curve));
                return true;
            }
        }

        return handled;
    }
}

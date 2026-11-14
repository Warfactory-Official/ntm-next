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
import com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import com.hbm.inventory.container.RBMKControlAutoMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RBMKAutoCurveLayout;
import com.hbm.packet.layout.RBMKAutoFunctionLayout;
import com.hbm.util.NumberFormatter;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class RBMKControlAutoScreen extends InfoContainerScreen<RBMKControlAutoMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/rbmk/gui_rbmk_control_auto.png");

    private final EditBox[] fields = new EditBox[4];

    public RBMKControlAutoScreen(
            RBMKControlAutoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    protected void init() {
        super.init();
        RBMKControlAutoBlockEntity be = be();
        for (int i = 0; i < 4; i++) {
            EditBox field =
                    new EditBox(
                            this.font,
                            leftPos + 30,
                            topPos + 27 + 11 * i,
                            26,
                            10,
                            Component.empty());
            field.setBordered(false);
            field.setMaxLength(i < 2 ? 3 : 4);
            field.setTextColor(CommonColors.WHITE);
            addRenderableWidget(field);
            fields[i] = field;
        }
        if (be != null) {
            fields[0].setValue(String.valueOf((int) be.levelUpper));
            fields[1].setValue(String.valueOf((int) be.levelLower));
            fields[2].setValue(String.valueOf((int) be.heatUpper));
            fields[3].setValue(String.valueOf((int) be.heatLower));
        }
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
        RBMKControlAutoBlockEntity be = be();
        double level = be != null ? be.extraction : 0;
        int height = (int) (56 * (1D - level));
        if (height > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    124,
                    29,
                    176,
                    56 - height,
                    8,
                    height,
                    256,
                    256);

        int f = be != null ? be.function.ordinal() : 0;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 59, 27, 184, f * 19, 26, 19, 256, 256);

        if (be != null && be.isPowered()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    136,
                    21,
                    210,
                    be.powerAvailable ? 16 : 0,
                    16,
                    16,
                    256,
                    256);
            if (checkClick(mouseX, mouseY, 136, 21, 16, 16)) {
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        136,
                        21,
                        16,
                        16,
                        List.of(
                                Component.literal(
                                        NumberFormatter.formatCompact(be.energy)
                                                + " / "
                                                + NumberFormatter.formatCompact(
                                                        RBMKControlBlockEntity.BUFFER)
                                                + "HE")));
            }
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                124,
                29,
                16,
                56,
                List.of(Component.literal((int) (level * 100) + "%")));
        if (be != null) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    58,
                    26,
                    28,
                    19,
                    List.of(
                            Component.translatable(
                                    "desc.gui.rbmkControlAuto.function",
                                    Component.translatable(be.function.nameKey()))));
        }
        for (ControlCurve curve : ControlCurve.VALUES) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    61,
                    48 + curve.ordinal() * 11,
                    22,
                    10,
                    List.of(Component.translatable(curve.selectKey())));
        }
        String[] field = {"levelUpper", "levelLower", "heatUpper", "heatLower"};
        for (int k = 0; k < field.length; k++) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    28,
                    26 + k * 11,
                    30,
                    10,
                    lineArray("desc.gui.rbmkControlAuto." + field[k]));
        }
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                28,
                70,
                30,
                10,
                List.of(Component.translatable("desc.gui.rbmkControlAuto.save")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int x = (int) event.x(), y = (int) event.y();
            if (checkClick(x, y, 28, 70, 30, 10)) {
                saveParameters();
                playClick();
                return true;
            }
            for (ControlCurve curve : ControlCurve.VALUES) {
                if (checkClick(x, y, 61, 48 + curve.ordinal() * 11, 22, 10)) {
                    BlockPos core = corePos();
                    if (core != null)
                        PacketWire.sendToServer(
                                RBMKAutoFunctionLayout.encode(menu.containerId, curve));
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void saveParameters() {
        BlockPos core = corePos();
        if (core == null) return;
        double[] vals = new double[4];
        for (int k = 0; k < 4; k++) {
            double clamp = k < 2 ? 100 : 9999;
            String text = fields[k].getValue();
            double v = 0;
            try {
                double parsed = Double.parseDouble(text);
                if (Double.isFinite(parsed)) v = Math.clamp(parsed, 0, clamp);
            } catch (NumberFormatException ignored) {
            }
            fields[k].setValue(String.valueOf((int) v));
            vals[k] = v;
        }
        PacketWire.sendToServer(
                RBMKAutoCurveLayout.encode(menu.containerId, vals[1], vals[0], vals[3], vals[2]));
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private RBMKControlAutoBlockEntity be() {
        return menu.blockEntity();
    }
}

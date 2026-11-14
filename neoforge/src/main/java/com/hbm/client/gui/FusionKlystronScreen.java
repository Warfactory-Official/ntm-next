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

import com.hbm.blockentity.machine.fusion.FusionKlystronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import com.hbm.inventory.container.FusionKlystronMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.FusionKlystronOutputLayout;
import com.hbm.util.NumberFormatter;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FusionKlystronScreen extends InfoContainerScreen<FusionKlystronMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/reactors/gui_fusion_klystron.png");

    private static final int POWER_X = 8, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int STAT_W = 18, STAT_H = 18, STAT_Y = 71;
    private static final int OUT_X = 43, AIR_X = 76, HE_X = 115;
    private static final int LED_Y = 71, LED_POWER_X = 160, LED_AIR_X = 170, LED_ACTION_X = 180;

    private EditBox field;

    public FusionKlystronScreen(FusionKlystronMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 200);
        this.inventoryLabelX = 35;
        this.inventoryLabelY = this.imageHeight - 93;
    }

    @Override
    protected int titleCenterX() {
        return 115;
    }

    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(font, leftPos + 84, topPos + 22, 102, 12, Component.empty());
        field.setTextColor(0xFF00FF00);
        field.setTextColorUneditable(0xFF00FF00);
        field.setBordered(false);
        field.setValue(Long.toString(menu.blockEntity().outputTarget));
        addRenderableWidget(field);
    }

    @Override
    protected void fieldKeyTaken(EditBox box) {
        sendTarget();
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        boolean typing = field.isFocused();
        boolean handled = super.charTyped(event);
        if (typing && handled) sendTarget();
        return handled;
    }

    private void sendTarget() {
        String text = field.getValue();
        if (text.startsWith("0")) field.setValue(text.substring(1));
        if (field.getValue().isEmpty()) field.setValue("0");
        text = field.getValue();
        if (!text.chars().allMatch(Character::isDigit)) return;

        long amount;
        try {
            amount = Long.parseLong(text);
        } catch (NumberFormatException e) {
            amount = 0L;
        }
        PacketWire.sendToServer(FusionKlystronOutputLayout.encode(menu.containerId, amount));
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
        FusionKlystronBlockEntity be = menu.blockEntity();

        long maxPower = be.getMaxPower();
        if (maxPower > 0) {
            int p = (int) (be.power * POWER_H / maxPower);
            if (p > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - p),
                        194,
                        POWER_H - p,
                        POWER_W,
                        p,
                        256,
                        256);
            }
        }

        double outputGauge =
                be.outputTarget <= 0 ? 0 : (double) be.output / (double) be.outputTarget;
        double airGauge = (double) be.compair.getFill() / (double) be.compair.getMaxFill();
        double powerGauge = FusionTorusBlockEntity.getSpeedScaled(be.maxPower, be.power);

        led(graphics, LED_POWER_X, powerGauge >= 0.5 && be.output > 0, powerGauge > 0);
        led(graphics, LED_AIR_X, airGauge >= 0.5 && be.output > 0, airGauge > 0);
        led(graphics, LED_ACTION_X, be.output >= be.outputTarget && be.output > 0, be.output > 0);

        SmoothGaugeElement.draw(graphics, 52, 80, outputGauge, 5, 2, 1, 0xA00000, 0x000000);
        SmoothGaugeElement.draw(graphics, 88, 80, airGauge, 5, 2, 1, 0xA00000, 0x000000);
        SmoothGaugeElement.draw(graphics, 124, 80, powerGauge, 5, 2, 1, 0xA00000, 0x000000);

        String result = "= " + NumberFormatter.formatCompact(be.outputTarget) + "KyU";
        if (be.outputTarget == FusionKlystronBlockEntity.MAX_OUTPUT) result += " (max)";
        graphics.text(
                font, Component.literal(result), 183 - font.width(result), 40, 0xFF00FF00, false);

        drawElectricityInfo(
                graphics, mouseX, mouseY, POWER_X, POWER_Y, POWER_W, POWER_H, be.power, maxPower);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                OUT_X,
                STAT_Y,
                STAT_W,
                STAT_H,
                List.of(
                        Component.literal(
                                ChatFormatting.RED
                                        + "<- "
                                        + ChatFormatting.RESET
                                        + NumberFormatter.formatCompact(be.output)
                                        + "KyU / "
                                        + NumberFormatter.formatCompact(be.outputTarget)
                                        + "KyU")));
        drawFluidGaugeInfo(graphics, mouseX, mouseY, AIR_X, STAT_Y, STAT_W, STAT_H, be.compair);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                HE_X,
                STAT_Y,
                STAT_W,
                STAT_H,
                List.of(
                        Component.literal(
                                ChatFormatting.GREEN
                                        + "-> "
                                        + ChatFormatting.RESET
                                        + NumberFormatter.formatCompact(be.output)
                                        + "HE / "
                                        + NumberFormatter.formatCompact(be.outputTarget)
                                        + "HE")));
        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void led(GuiGraphicsExtractor graphics, int x, boolean lit, boolean on) {
        if (lit)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, LED_Y, 210, 8, 8, 8, 256, 256);
        else if (on)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, LED_Y, 210, 0, 8, 8, 256, 256);
    }
}

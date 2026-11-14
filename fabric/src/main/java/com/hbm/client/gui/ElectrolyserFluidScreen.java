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

import com.hbm.blockentity.processing.ElectrolysisBlockEntity;
import com.hbm.inventory.container.ElectrolyserFluidMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ElectrolyserOpenMetalMenuLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ElectrolyserFluidScreen extends InfoContainerScreen<ElectrolyserFluidMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_electrolyser_fluid.png");

    public ElectrolyserFluidScreen(
            ElectrolyserFluidMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 210, 204);
        this.titleLabelY = 7;
    }

    private ElectrolysisBlockEntity be() {
        return menu.blockEntity();
    }

    @Override
    protected int titleCenterX() {
        return imageWidth / 2 - 16;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ElectrolysisBlockEntity be = be();
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

        int p = (int) (be.power * 89 / ElectrolysisBlockEntity.MAX_POWER);
        if (p > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    186,
                    107 - p,
                    210,
                    89 - p,
                    16,
                    p,
                    256,
                    256);

        if (be.power >= be.usageFluid) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 190, 4, 226, 40, 9, 12, 256, 256);
        }

        int time = be.processFluidTime > 0 ? be.processFluidTime : 1;
        int e = be.progressFluid * 41 / time;
        if (e > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 62, 26, 226, 0, 12, e, 256, 256);

        drawFluidBar(graphics, 42, 18, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 96, 18, 16, 52, be.tanks[1]);
        drawFluidBar(graphics, 116, 18, 16, 52, be.tanks[2]);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 42, 18, 16, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 96, 18, 16, 52, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 116, 18, 16, 52, be.tanks[2]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                186,
                18,
                16,
                89,
                be.power,
                ElectrolysisBlockEntity.MAX_POWER);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), 8, 82, 54, 12)) {
            PacketWire.sendToServer(ElectrolyserOpenMetalMenuLayout.encode(menu.containerId));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}

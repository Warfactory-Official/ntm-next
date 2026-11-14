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

import com.hbm.blockentity.machine.WatzBlockEntity;
import com.hbm.inventory.container.WatzMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.WatzLockLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class WatzScreen extends InfoContainerScreen<WatzMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/reactors/gui_watz.png");

    public WatzScreen(WatzMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 229);
        this.inventoryLabelY = this.imageHeight - 93;
    }

    private WatzBlockEntity be() {
        return menu.blockEntity();
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        WatzBlockEntity be = be();
        float col = Mth.clamp(1 - (float) Math.log(be.heat / 100_000D + 1) * 0.4F, 0F, 1F);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0,
                0,
                131,
                122,
                256,
                256,
                ARGB.colorFromFloat(1F, 1F, col, col));
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 131, 0, 131, 0, 36, 122, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 130, 0, 130, imageWidth, 99, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 126, 31, 176, 31, 9, 60, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 105, 96, 185, 26, 30, 26, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 9, 96, 184, 0, 26, 26, 256, 256);

        if (be.isOn)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 147, 8, 176, 0, 8, 8, 256, 256);
        if (be.isLocked)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 142, 70, 210, 0, 18, 18, 256, 256);

        SmoothGaugeElement.draw(graphics, 22, 109, 1D - col, 5, 2, 1, 0x7F0000, 0);

        drawFluidBar(graphics, 143, 26, 4, 43, be.tanks[0]);
        drawFluidBar(graphics, 149, 26, 4, 43, be.tanks[1]);
        drawFluidBar(graphics, 155, 26, 4, 43, be.tanks[2]);

        graphics.pose().pushMatrix();
        graphics.pose().scale(1F / 1.25F, 1F / 1.25F);
        String flux = String.format(Locale.ROOT, "%,.1f", be.fluxDisplay);

        graphics.text(
                this.font,
                flux,
                (int) (161 * 1.25F - this.font.width(flux)),
                (int) (107 * 1.25F),
                0xFF00FF00,
                false);
        graphics.pose().popMatrix();

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                13,
                100,
                18,
                18,
                List.of(Component.literal(String.format(Locale.ROOT, "%,d", be.heat) + " TU")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                143,
                71,
                16,
                16,
                List.of(
                        Component.literal(
                                be.isLocked
                                        ? "Unlock pellet IO configuration"
                                        : "Lock pellet IO configuration")));

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 142, 23, 6, 45, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 148, 23, 6, 45, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 154, 23, 6, 45, be.tanks[2]);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (checkClick((int) event.x(), (int) event.y(), 142, 70, 18, 18)) {
            PacketWire.sendToServer(WatzLockLayout.encode(menu.containerId));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}

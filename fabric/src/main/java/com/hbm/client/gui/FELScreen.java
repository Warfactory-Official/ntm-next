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

import com.hbm.blockentity.machine.WavelengthLaserBlockEntity;
import com.hbm.inventory.container.FELMenu;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.FELToggleLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.awt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class FELScreen extends InfoContainerScreen<FELMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/machine/gui_fel.png");

    public FELScreen(FELMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 203, 169);
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 98;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 + 90;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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

        var fel = fel();

        if (fel.enabled) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 142, 41, 203, 0, 29, 17, 256, 256);
        }

        int k = (int) fel.getPowerScaled(114);
        if (k > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    182,
                    27 + 113 - k,
                    203,
                    17 + 113 - k,
                    16,
                    k,
                    256,
                    256);
        }

        if (fel.energy > fel.feedbackThreshold()
                && fel.enabled
                && fel.wavelength != EnumWavelengths.NULL
                && fel.reach > 0) {
            int color =
                    fel.wavelength != EnumWavelengths.VISIBLE
                            ? fel.wavelength.guiColor
                            : Color.HSBtoRGB(gameTime() / 50.0F, 0.5F, 1F);
            int argb = ARGB.opaque(color);

            graphics.fill(113, 29, 135, 34, argb);
            graphics.fill(-leftPos, 29, 4, 34, argb);
        }

        drawElectricityInfo(
                graphics, mouseX, mouseY, 182, 27, 16, 113, fel.energy, fel.getMaxPower());

        this.titleLabelX = 90 + this.imageWidth / 2 - this.font.width(this.title) / 2;
        if (fel.receiverMissing && fel.enabled) {
            graphics.text(
                    this.font,
                    "ERR.",
                    55 + this.imageWidth / 2 - this.font.width(this.title) / 2,
                    9,
                    0xFFFF0000,
                    false);
        } else if (fel.enabled) {
            graphics.text(
                    this.font,
                    "LIVE",
                    54 + this.imageWidth / 2 - this.font.width(this.title) / 2,
                    9,
                    0xFF00FF00,
                    false);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private long gameTime() {
        Minecraft mc = this.minecraft;
        return mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), 142, 41, 29, 17)) {
            BlockPos core = corePos();
            if (core != null) {
                PacketWire.sendToServer(FELToggleLayout.encode(menu.containerId));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private @Nullable BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private WavelengthLaserBlockEntity fel() {
        return menu.blockEntity();
    }
}

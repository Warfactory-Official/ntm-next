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

import com.hbm.blockentity.machine.SilexBlockEntity;
import com.hbm.client.gui.InfoContainerScreen;
import com.hbm.inventory.container.SILEXMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.SilexVoidLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.awt.*;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class SILEXScreen extends InfoContainerScreen<SILEXMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_silex.png");

    public SILEXScreen(SILEXMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.titleLabelY = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 54;
    }

    public static float waveFrequency(EnumWavelengths mode) {
        return 0.1F * (float) Math.pow(2, mode.ordinal());
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

        SilexBlockEntity be = silex();

        if (be.wavelength() != EnumWavelengths.NULL) {
            float freq = waveFrequency(be.wavelength());
            int color =
                    be.wavelength() != EnumWavelengths.VISIBLE
                            ? be.wavelength().guiColor
                            : Color.HSBtoRGB(gameTime() / 50.0F, 0.5F, 1F);
            drawWave(graphics, 81, 46, 16, 84, 0.5F, freq, color);
        }

        boolean peroxide = be.reactionTank().getTankType() == NTMFluids.PEROXIDE;
        if (be.reactionTank().getFill() > 0) {
            int v =
                    peroxide
                                    || SILEXRecipes.INSTANCE.getOutput(
                                                    be.reactionTank().getTankType())
                                            != null
                            ? 118
                            : 109;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 41, 176, v, 54, 9, 256, 256);
        }

        int p = be.progressScaled(69);
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 45, 82, 176, 0, p, 43, 256, 256);

        int f = be.tankScaled(52);
        if (f > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    26,
                    124 - f,
                    176,
                    109 - f,
                    16,
                    f,
                    256,
                    256);

        int i = be.batchScaled(52);
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    8,
                    42,
                    176,
                    peroxide ? 43 : 50,
                    i,
                    7,
                    256,
                    256);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 42, 52, 7, be.reactionTank());

        if (be.batchItem() != null || be.batchFluid() != null) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    27,
                    72,
                    16,
                    52,
                    List.of(
                            Component.literal(be.batchAmount() + "/" + be.batchCapacity() + "mB"),
                            be.batchFluid() != null
                                    ? NTMFluidProperties.getDisplayName(be.batchFluid())
                                    : be.batchItem().getHoverName()));
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                10,
                92,
                10,
                10,
                List.of(Component.translatable("desc.gui.machineSILEX.voidContents")));

        if (be.wavelength() != EnumWavelengths.NULL) {
            Component modeName =
                    Component.translatable(be.wavelength().name)
                            .withStyle(be.wavelength().textColor);
            graphics.text(
                    this.font,
                    modeName,
                    132 - this.font.width(modeName) / 2,
                    16,
                    CommonColors.BLACK,
                    false);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawWave(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int height,
            int width,
            float resolution,
            float freq,
            int color) {
        float samples = width / resolution;
        float scale = height / 2F;
        float offset = (float) (gameTime() % (long) (4 * Math.PI / freq));
        int argb = ARGB.opaque(color);

        for (int i = 1; i < samples; i++) {
            double currentX = offset + x + i * resolution;
            double nextX = offset + x + (i + 1) * resolution;
            double currentY = y + scale * Math.sin(freq * currentX);
            double nextY = y + scale * Math.sin(freq * nextX);
            drawSegment(graphics, argb, currentX - offset, currentY, nextX - offset, nextY, 3F);
        }
    }

    private void drawSegment(
            GuiGraphicsExtractor graphics,
            int argb,
            double x1,
            double y1,
            double x2,
            double y2,
            float thickness) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float len = Mth.length(dx, dy);
        if (len <= 0) return;
        int h = Math.max(1, Math.round(thickness));
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) x1, (float) y1);
        pose.rotate((float) Math.atan2(dy, dx));
        graphics.fill(0, -h / 2, Math.max(1, Math.round(len)), h - h / 2, argb);
        pose.popMatrix();
    }

    private long gameTime() {
        Minecraft mc = this.minecraft;
        return mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), 10, 92, 12, 12)) {
            BlockPos core = corePos();
            if (core != null) {
                PacketWire.sendToServer(SilexVoidLayout.encode(menu.containerId));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private @Nullable BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private SilexBlockEntity silex() {
        return menu.blockEntity();
    }
}

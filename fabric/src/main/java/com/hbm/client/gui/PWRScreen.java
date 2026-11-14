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

import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.inventory.container.PWRMenu;
import com.hbm.items.ModItems;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PWRRodTargetLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PWRScreen extends InfoContainerScreen<PWRMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/reactors/gui_pwr.png");

    private EditBox rodField;

    public PWRScreen(PWRMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 188);
    }

    private static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    @Override
    protected void init() {
        super.init();
        rodField = new EditBox(this.font, leftPos + 57, topPos + 63, 30, 8, Component.empty());
        rodField.setMaxLength(3);
        rodField.setBordered(false);
        rodField.setTextColor(CommonColors.GREEN);
        PWRControllerBlockEntity be = be();
        rodField.setValue(String.valueOf(be != null ? (int) (100 - be.rodTarget) : 100));
        addRenderableWidget(rodField);
    }

    @Override
    protected boolean drawTitle() {
        return false;
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
        PWRControllerBlockEntity be = be();
        if (be != null) {
            if (be.hullHeat > PWRControllerBlockEntity.coreHeatCapacityBase * 0.8
                    || be.coreHeat > be.coreHeatCapacity * 0.8) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 147, 0, 176, 14, 26, 26, 256, 256);
            }
            if (be.processTime > 0) {
                int p = (int) (be.progress * 33 / be.processTime);
                if (p > 0)
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            54,
                            33,
                            176,
                            0,
                            Math.min(p, 33),
                            14,
                            256,
                            256);
            }
            int c = (int) (be.rodLevel * 52 / 100);
            if (c > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 53, 54, 176, 40, c, 2, 256, 256);

            SmoothGaugeElement.draw(
                    graphics,
                    124,
                    40,
                    (double) be.coreHeat / be.coreHeatCapacity,
                    5,
                    2,
                    1,
                    0x7F0000,
                    0x000000);
            SmoothGaugeElement.draw(
                    graphics,
                    160,
                    40,
                    (double) be.hullHeat / PWRControllerBlockEntity.coreHeatCapacityBase,
                    5,
                    2,
                    1,
                    0x7F0000,
                    0x000000);

            if (be.typeLoaded >= 0
                    && be.typeLoaded < ModItems.PWR_FUELS.size()
                    && be.amountLoaded > 0) {
                int x = 89;
                int y = 5;
                ItemStack fuel = new ItemStack(ModItems.PWR_FUELS.get(be.typeLoaded).get());
                String label = ChatFormatting.YELLOW + "" + be.amountLoaded + "/" + be.rodCount;
                graphics.item(fuel, x, y);
                graphics.pose().pushMatrix();
                graphics.pose().scale(0.5F, 0.5F);
                graphics.itemDecorations(
                        this.font, fuel, (x + this.font.width(label) / 4) * 2, (y + 15) * 2, label);
                graphics.pose().popMatrix();
            }

            graphics.pose().pushMatrix();
            graphics.pose().scale(1F / 1.25F, 1F / 1.25F);
            String fluxStr = String.format(Locale.ROOT, "%,.1f", be.flux);

            graphics.text(
                    this.font,
                    fluxStr,
                    (int) (165 * 1.25F - this.font.width(fluxStr)),
                    (int) (64 * 1.25F),
                    0xFF00FF00,
                    false);
            graphics.pose().popMatrix();

            drawFluidBar(graphics, 8, 5, 16, 52, be.tanks[0]);
            drawFluidBar(graphics, 26, 5, 16, 52, be.tanks[1]);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 5, 16, 52, be.tanks[0]);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 26, 5, 16, 52, be.tanks[1]);

            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    115,
                    31,
                    18,
                    18,
                    List.of(
                            Component.translatable(
                                    "desc.gui.pwr.core",
                                    fmt(be.coreHeat),
                                    fmt(be.coreHeatCapacity))));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    151,
                    31,
                    18,
                    18,
                    List.of(
                            Component.translatable(
                                    "desc.gui.pwr.hull",
                                    fmt(be.hullHeat),
                                    fmt(PWRControllerBlockEntity.coreHeatCapacityBase))));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    52,
                    31,
                    36,
                    18,
                    List.of(
                            Component.literal(
                                    (be.processTime > 0
                                                    ? (int) (be.progress * 100 / be.processTime)
                                                    : 0)
                                            + "%")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    52,
                    53,
                    54,
                    4,
                    List.of(
                            Component.translatable(
                                    "desc.gui.pwr.controlRodWithdrawal",
                                    100 - Math.round(be.rodLevel)),
                            Component.translatable(
                                    "desc.gui.pwr.flux",
                                    String.format(Locale.ROOT, "%,.1f", be.flux))));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), 88, 58, 18, 18)) {
            sendRodLevel();
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void sendRodLevel() {
        BlockPos core = corePos();
        if (core == null || rodField == null) return;
        int withdrawal;
        try {
            withdrawal = Math.clamp(Integer.parseInt(rodField.getValue().trim()), 0, 100);
        } catch (NumberFormatException e) {
            return;
        }
        rodField.setValue(String.valueOf(withdrawal));
        PacketWire.sendToServer(PWRRodTargetLayout.encode(menu.containerId, 100 - withdrawal));
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private PWRControllerBlockEntity be() {
        return menu.blockEntity();
    }
}

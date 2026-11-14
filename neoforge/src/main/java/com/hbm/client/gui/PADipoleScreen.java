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

import com.hbm.blockentity.machine.albion.PADipoleBlockEntity;
import com.hbm.inventory.container.PADipoleMenu;
import com.hbm.items.machine.PACoilItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PADipoleDirectionLayout;
import com.hbm.packet.layout.PADipoleThresholdLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class PADipoleScreen extends PACooledScreen<PADipoleMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/particleaccelerator/gui_dipole.png");

    private static final int BUTTON_X = 62;
    private static final int[] BUTTON_Y = {29, 43, 57};
    private static final int NEEDLE_X = 68;
    private static final int[] NEEDLE_Y = {35, 49, 63};
    private @Nullable EditBox threshold;

    public PADipoleScreen(PADipoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 9;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
    }

    @Override
    protected void init() {
        super.init();
        threshold = new EditBox(this.font, leftPos + 47, topPos + 77, 66, 8, Component.empty());
        threshold.setBordered(false);
        threshold.setMaxLength(9);
        threshold.setTextColor(0xFF00FF00);
        threshold.setTextColorUneditable(0xFF00FF00);
        threshold.setValue(String.valueOf(menu.blockEntity().threshold));
        addRenderableWidget(threshold);
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
        PADipoleBlockEntity be = menu.blockEntity();

        drawPowerBar(graphics, TEXTURE, 8, be.power, be.getMaxPower());

        if (be.power >= PADipoleBlockEntity.usage) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 83, 54, 176, 8, 8, 8, 256, 256);
        }
        if (Math.ceil(be.temperature) <= COLD_ENOUGH) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 93, 54, 176, 8, 8, 8, 256, 256);
        }
        PACoilItem.EnumCoilType coil = PACoilItem.typeOf(be.getItem(PADipoleBlockEntity.SLOT_COIL));
        if (coil != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 103, 54, 176, 8, 8, 8, 256, 256);
            int u =
                    coil == PACoilItem.EnumCoilType.GOLD || coil == PACoilItem.EnumCoilType.BSCCO
                            ? 200
                            : 228;
            int v =
                    coil == PACoilItem.EnumCoilType.GOLD || coil == PACoilItem.EnumCoilType.NIOBIUM
                            ? 96
                            : 124;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 83, 20, u, v, 28, 28, 256, 256);
        }

        float playerYaw =
                this.minecraft != null && this.minecraft.player != null
                        ? this.minecraft.player.getYRot()
                        : 0F;
        int[] dirs = {be.dirLower, be.dirUpper, be.dirRedstone};
        for (int i = 0; i < 3; i++) {
            drawNeedle(graphics, NEEDLE_X, NEEDLE_Y[i], 0x8080FF, 180F);
            drawNeedle(graphics, NEEDLE_X, NEEDLE_Y[i], 0xFF0000, playerYaw - dirs[i] * 90F);
        }

        drawCoolant(graphics, mouseX, mouseY, be, 134);
        drawElectricityInfo(graphics, mouseX, mouseY, 8, 18, 16, 52, be.power, be.getMaxPower());

        for (int i = 0; i < 3; i++) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    BUTTON_X,
                    BUTTON_Y[i],
                    12,
                    12,
                    List.of(
                            Component.translatable("desc.gui.paDipole.playerOrientation")
                                    .withStyle(ChatFormatting.BLUE),
                            Component.translatable("desc.gui.paDipole.outputOrientation")
                                    .withStyle(ChatFormatting.RED),
                            Component.literal(PADipoleBlockEntity.ditToDirection(dirs[i]).name())));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawNeedle(
            GuiGraphicsExtractor graphics, int x, int y, int color, float yawDegrees) {
        double theta = Math.toRadians(yawDegrees);
        float dx = (float) (6D * Math.sin(theta));
        float dy = (float) (6D * Math.cos(theta));

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.rotate((float) Math.atan2(dy, dx));
        graphics.fill(0, -1, 6, 2, ARGB.opaque(color));
        pose.popMatrix();
    }

    @Override
    protected void fieldKeyTaken(EditBox box) {
        sendThreshold(box);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        EditBox box = this.threshold;
        boolean typing = box != null && box.isFocused();
        boolean handled = super.charTyped(event);
        if (typing && handled) sendThreshold(box);
        return handled;
    }

    private void sendThreshold(EditBox box) {
        String text = box.getValue();
        if (text.startsWith("0")) box.setValue(text.substring(1));
        if (box.getValue().isEmpty()) box.setValue("0");
        text = box.getValue();
        if (!text.chars().allMatch(Character::isDigit)) return;
        PacketWire.sendToServer(
                PADipoleThresholdLayout.encode(menu.containerId, Integer.parseInt(text)));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < 3; i++) {
                if (checkClick((int) event.x(), (int) event.y(), BUTTON_X, BUTTON_Y[i], 12, 12)) {
                    PacketWire.sendToServer(
                            PADipoleDirectionLayout.encode(
                                    menu.containerId,
                                    PADipoleBlockEntity.DirectionControl.values()[i]));
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

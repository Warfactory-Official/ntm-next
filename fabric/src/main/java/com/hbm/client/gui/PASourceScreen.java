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

import com.hbm.blockentity.machine.albion.PASourceBlockEntity;
import com.hbm.inventory.container.PASourceMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PASourceCancelLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class PASourceScreen extends PACooledScreen<PASourceMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/particleaccelerator/gui_source.png");

    public PASourceScreen(PASourceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 4;
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PASourceBlockEntity be = menu.blockEntity();

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
        drawPowerBar(graphics, TEXTURE, 8, be.power, be.getMaxPower());

        if (Math.ceil(be.temperature) <= COLD_ENOUGH) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 44, 16, 176, 8, 8, 8, 256, 256);
        }
        if (be.power >= PASourceBlockEntity.usage) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 44, 41, 176, 8, 8, 8, 256, 256);
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                45,
                73,
                176,
                52,
                68,
                14,
                256,
                256,
                ARGB.opaque(be.state.color));

        drawCoolant(graphics, mouseX, mouseY, be, 134);
        drawElectricityInfo(graphics, mouseX, mouseY, 8, 18, 16, 52, be.power, be.getMaxPower());

        List<Component> info = new ArrayList<>();
        info.add(
                Component.translatable("desc.gui.paSource.lastMomentum")
                        .withStyle(ChatFormatting.BLUE)
                        .append(
                                Component.literal(String.format(Locale.ROOT, "%,d", be.lastSpeed))
                                        .withStyle(ChatFormatting.RESET)));
        for (Component line :
                lineArray("pa." + be.state.name().toLowerCase(Locale.ROOT) + ".desc")) {
            info.add(line.copy().withStyle(ChatFormatting.YELLOW));
        }
        drawCustomInfoStat(graphics, mouseX, mouseY, 105, 16, 10, 10, info);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                105,
                28,
                10,
                10,
                List.of(
                        Component.translatable("desc.gui.paSource.cancelOperation")
                                .withStyle(ChatFormatting.RED)));

        Component state = Component.translatable("pa." + be.state.name().toLowerCase(Locale.ROOT));
        graphics.text(
                this.font,
                state,
                79 - this.font.width(state) / 2,
                76,
                ARGB.opaque(be.state.color),
                false);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), 105, 28, 10, 10)) {
            PacketWire.sendToServer(PASourceCancelLayout.encode(menu.containerId));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}

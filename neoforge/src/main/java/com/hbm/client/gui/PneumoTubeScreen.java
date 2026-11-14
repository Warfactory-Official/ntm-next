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

import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blockentity.network.pneumatic.PneumaticTubeBlockEntity;
import com.hbm.inventory.container.PneumoTubeMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PneumoTubePressureLayout;
import com.hbm.packet.layout.PneumoTubeReceiveOrderLayout;
import com.hbm.packet.layout.PneumoTubeRedstoneLayout;
import com.hbm.packet.layout.PneumoTubeSendOrderLayout;
import com.hbm.packet.layout.PneumoTubeWhitelistLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class PneumoTubeScreen extends InfoContainerScreen<PneumoTubeMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_pneumatic_pipe.png");
    private static final Identifier TEXTURE_ENDPOINT =
            RegistryUtil.id("textures/gui/storage/gui_pneumatic_endpoint.png");

    public PneumoTubeScreen(PneumoTubeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private boolean endpointOnly() {
        return !menu.blockEntity().hasIntake();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PneumaticTubeBlockEntity tube = menu.blockEntity();
        boolean endpointOnly = endpointOnly();

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                endpointOnly ? TEXTURE_ENDPOINT : TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        if (tube.matchOnly) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 139, 33, 176, 0, 3, 6, 256, 256);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 139, 47, 176, 0, 3, 6, 256, 256);
        }

        if (!endpointOnly) {
            if (tube.requirePower) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 52, 179, 0, 18, 18, 256, 256);
            }
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    151,
                    16,
                    197,
                    18 * tube.destinationOrder,
                    18,
                    18,
                    256,
                    256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    151,
                    52,
                    215,
                    18 * tube.sourceOrder,
                    18,
                    18,
                    256,
                    256);

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    6 + 4 * (tube.gasTank.getPressure() - 1),
                    36,
                    179,
                    18,
                    4,
                    8,
                    256,
                    256);
            SmoothGaugeElement.draw(
                    graphics,
                    16,
                    25,
                    (double) tube.gasTank.getFill() / tube.gasTank.getMaxFill(),
                    5,
                    2,
                    1,
                    0xFFCA6C43,
                    0xFFAB4223);

            drawFluidGaugeInfo(graphics, mouseX, mouseY, 7, 16, 18, 18, tube.gasTank);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    7,
                    52,
                    18,
                    18,
                    List.of(
                            Component.literal(
                                    (tube.requirePower
                                                    ? ChatFormatting.GREEN + "ON "
                                                    : ChatFormatting.RED + "OFF ")
                                            + ChatFormatting.RESET
                                            + "with Redstone")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    6,
                    36,
                    20,
                    8,
                    List.of(
                            Component.translatable(
                                    "desc.shared.compressor", tube.gasTank.getPressure()),
                            Component.translatable(
                                    "desc.shared.maxRange",
                                    PneumaticTubeBlockEntity.rangeForPressure(
                                            tube.gasTank.getPressure()))));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    151,
                    16,
                    18,
                    18,
                    List.of(
                            Component.literal(ChatFormatting.YELLOW + "Receiver order:"),
                            Component.literal(
                                    tube.destinationOrder == PneumaticNetwork.RECEIVE_ROBIN
                                            ? "Round robin"
                                            : "Random")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    151,
                    52,
                    18,
                    18,
                    List.of(
                            Component.literal(ChatFormatting.YELLOW + "Provider slot order:"),
                            Component.literal(
                                    tube.sourceOrder == PneumaticNetwork.SEND_FIRST
                                            ? "First to last"
                                            : tube.sourceOrder == PneumaticNetwork.SEND_LAST
                                                    ? "Last to first"
                                                    : "Random")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> slotTooltip(Slot slot) {
        return slot.index < PneumaticTubeBlockEntity.SLOT_COUNT
                ? patternModeLines(menu.blockEntity().filterPatterns.mode(slot.index))
                : List.of();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        int mx = (int) x, my = (int) y;

        if (!endpointOnly()) {
            clickSendFlag(mx, my, 7, 52, 18, 18, "redstone");
            clickSendFlag(mx, my, 6, 36, 20, 8, "pressure");
            clickSendFlag(mx, my, 151, 16, 18, 18, "receive");
            clickSendFlag(mx, my, 151, 52, 18, 18, "send");
        }

        clickSendFlag(mx, my, 128, 30, 14, 26, "whitelist");

        return super.mouseClicked(event, doubleClick);
    }

    private void clickSendFlag(
            int mouseX, int mouseY, int left, int top, int sizeX, int sizeY, String key) {
        if (!checkClick(mouseX, mouseY, left, top, sizeX, sizeY)) return;
        switch (key) {
            case "redstone" ->
                    PacketWire.sendToServer(PneumoTubeRedstoneLayout.encode(menu.containerId));
            case "pressure" ->
                    PacketWire.sendToServer(PneumoTubePressureLayout.encode(menu.containerId));
            case "receive" ->
                    PacketWire.sendToServer(PneumoTubeReceiveOrderLayout.encode(menu.containerId));
            case "send" ->
                    PacketWire.sendToServer(PneumoTubeSendOrderLayout.encode(menu.containerId));
            case "whitelist" ->
                    PacketWire.sendToServer(PneumoTubeWhitelistLayout.encode(menu.containerId));
            default -> throw new IllegalArgumentException(key);
        }
        playClick();
    }
}

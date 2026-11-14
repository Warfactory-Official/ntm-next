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

import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKManualControlBlockEntity;
import com.hbm.inventory.container.RBMKControlMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RBMKManualColorLayout;
import com.hbm.packet.layout.RBMKManualTargetLayout;
import com.hbm.util.NumberFormatter;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class RBMKControlScreen extends InfoContainerScreen<RBMKControlMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/rbmk/gui_rbmk_control.png");

    public RBMKControlScreen(RBMKControlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
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

        RBMKControlBlockEntity be = be();
        double level = be != null ? be.extraction : 0;
        int height = (int) (56 * (1D - level));
        if (height > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    75,
                    29,
                    176,
                    56 - height,
                    8,
                    height,
                    256,
                    256);

        if (be != null && be.isPowered()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    87,
                    21,
                    196,
                    be.powerAvailable ? 16 : 0,
                    16,
                    16,
                    256,
                    256);
            if (checkClick(mouseX, mouseY, 87, 21, 16, 16)) {
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        87,
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
                71,
                29,
                16,
                56,
                List.of(Component.literal((int) (level * 100) + "%")));

        if (be instanceof RBMKManualControlBlockEntity m && m.group != null) {
            int color = m.group.ordinal();
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    28,
                    26 + color * 11,
                    184,
                    color * 10,
                    12,
                    10,
                    256,
                    256);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x(), my = (int) event.y();
            for (int k = 0; k < 5; k++) {
                if (checkClick(mx, my, 118, 26 + k * 11, 30, 10)) {
                    sendLevel(1.0D - (k * 0.25D));
                    playClick();
                    return true;
                }
                if (checkClick(mx, my, 28, 26 + k * 11, 12, 10)) {
                    sendColor(k);
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void sendLevel(double level) {
        BlockPos core = corePos();
        if (core == null) return;
        PacketWire.sendToServer(RBMKManualTargetLayout.encode(menu.containerId, level));
    }

    private void sendColor(int ordinal) {
        BlockPos core = corePos();
        if (core == null) return;
        PacketWire.sendToServer(RBMKManualColorLayout.encode(menu.containerId, ordinal));
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private RBMKControlBlockEntity be() {
        return menu.blockEntity();
    }
}

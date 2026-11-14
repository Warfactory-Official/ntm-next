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

import com.hbm.blockentity.machine.rbmk.RBMKBoilerBlockEntity;
import com.hbm.inventory.container.RBMKBoilerMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RBMKBoilerCompressorLayout;
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
import net.minecraft.world.level.material.Fluid;

public class RBMKBoilerScreen extends InfoContainerScreen<RBMKBoilerMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/rbmk/gui_rbmk_boiler.png");

    public RBMKBoilerScreen(RBMKBoilerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    private static int steamTypeU(Fluid type) {
        if (type == NTMFluids.HOTSTEAM) return 208;
        if (type == NTMFluids.SUPERHOTSTEAM) return 222;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 236;
        return 194;
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

        RBMKBoilerBlockEntity be = be();
        if (be != null) {
            int i = be.feed.getMaxFill() > 0 ? be.feed.getFill() * 58 / be.feed.getMaxFill() : 0;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    126,
                    82 - i,
                    176,
                    58 - i,
                    14,
                    i,
                    256,
                    256);

            int j = be.steam.getMaxFill() > 0 ? be.steam.getFill() * 22 / be.steam.getMaxFill() : 0;
            if (j > 0) j++;
            if (j > 22) j++;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 91, 65 - j, 190, 24 - j, 4, j, 256, 256);

            int u = steamTypeU(be.steam.getTankType());
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 36, 24, u, 0, 14, 58, 256, 256);

            drawFluidGaugeInfo(graphics, mouseX, mouseY, 126, 24, 16, 56, be.feed);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 89, 39, 8, 28, be.steam);
            if (checkClick(mouseX, mouseY, 33, 21, 20, 64)) {
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        33,
                        21,
                        20,
                        64,
                        List.of(
                                Component.translatable(
                                        "desc.gui.rbmkBoiler.compressSteamCycleTier"),
                                Component.translatable(
                                        "desc.gui.rbmkBoiler.columnHeat", (int) be.heat + " °C")));
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), 33, 21, 20, 64)) {
            BlockPos core = corePos();
            if (core != null) {
                PacketWire.sendToServer(RBMKBoilerCompressorLayout.encode(menu.containerId));
                playClick();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private RBMKBoilerBlockEntity be() {
        return menu.blockEntity();
    }
}

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

import com.hbm.blockentity.machine.ContinuousCaster;
import com.hbm.client.ModifierKeys;
import com.hbm.client.render.HbmRenderPipelines;
import com.hbm.inventory.container.StrandCasterMenu;
import com.hbm.inventory.material.MaterialTooltip;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class StrandCasterScreen extends InfoContainerScreen<StrandCasterMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_strand_caster.png");

    public StrandCasterScreen(StrandCasterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 214);
        titleLabelY = 4;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return 0xFFFFFFFF;
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

        ContinuousCaster be = caster();

        if (be.amount != 0 && be.type != null) {
            int targetHeight = Math.min(be.amount * 79 / be.getCapacity(), 92);
            if (targetHeight > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        17,
                        93 - targetHeight,
                        176,
                        89 - targetHeight,
                        34,
                        targetHeight,
                        256,
                        256,
                        ARGB.opaque(be.type.moltenColor()));
                graphics.blit(
                        HbmRenderPipelines.GUI_TEXTURED_ADDITIVE,
                        TEXTURE,
                        17,
                        93 - targetHeight,
                        176,
                        89 - targetHeight,
                        34,
                        targetHeight,
                        256,
                        256,
                        0x4CFFFFFF);
            }
        }

        drawFluidBar(graphics, 82, 14, 16, 24, be.water);
        drawFluidBar(graphics, 82, 65, 16, 24, be.steam);

        drawStackInfo(graphics, mouseX, mouseY, 16, 17);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 82, 14, 16, 24, be.water);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 82, 65, 16, 24, be.steam);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawStackInfo(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y) {
        ContinuousCaster be = caster();
        List<Component> list;

        if (be.type == null) {
            list =
                    List.of(
                            Component.translatable("desc.shared.empty")
                                    .withStyle(ChatFormatting.RED));
        } else {
            list =
                    List.of(
                            Component.empty()
                                    .append(be.type.displayName())
                                    .append(Component.literal(": "))
                                    .append(
                                            MaterialTooltip.of(
                                                    be.amount, ModifierKeys.detailsHeld()))
                                    .withStyle(ChatFormatting.YELLOW));
        }

        drawCustomInfoStat(graphics, mouseX, mouseY, x, y, 36, 81, list);
    }

    private ContinuousCaster caster() {
        return menu.blockEntity();
    }
}

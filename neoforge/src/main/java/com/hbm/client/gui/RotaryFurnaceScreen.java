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

import com.hbm.blockentity.machine.RotaryFurnaceBlockEntity;
import com.hbm.client.ModifierKeys;
import com.hbm.client.render.HbmRenderPipelines;
import com.hbm.inventory.container.RotaryFurnaceMenu;
import com.hbm.inventory.material.MaterialTooltip;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class RotaryFurnaceScreen extends InfoContainerScreen<RotaryFurnaceMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_rotary_furnace.png");

    public RotaryFurnaceScreen(RotaryFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        titleLabelY = 6;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return (imageWidth - 54) / 2;
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

        RotaryFurnaceBlockEntity be = furnace();

        int p = (int) Math.ceil(be.progress * 33);
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 63, 30, 176, 0, p, 10, 256, 256);

        if (be.maxBurnTime > 0 && be.burnTime > 0) {
            int b = be.burnTime * 14 / be.maxBurnTime;
            if (b > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        26,
                        69 - b,
                        176,
                        24 - b,
                        14,
                        b,
                        256,
                        256);
        }

        if (be.output != null) {
            int amount = be.output.amount * 52 / RotaryFurnaceBlockEntity.MAX_OUTPUT;
            if (amount > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        98,
                        70 - amount,
                        176,
                        76 - amount,
                        16,
                        amount,
                        256,
                        256,
                        ARGB.opaque(be.output.material.moltenColor()));
                graphics.blit(
                        HbmRenderPipelines.GUI_TEXTURED_ADDITIVE,
                        TEXTURE,
                        98,
                        70 - amount,
                        176,
                        76 - amount,
                        16,
                        amount,
                        256,
                        256,
                        0x4CFFFFFF);
            }
        }

        drawFluidBarH(graphics, 8, 36, 52, 16, be.tanks[0]);
        drawFluidBar(graphics, 134, 18, 16, 52, be.tanks[1]);
        drawFluidBar(graphics, 152, 18, 16, 52, be.tanks[2]);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 36, 52, 16, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 134, 18, 16, 52, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 152, 18, 16, 52, be.tanks[2]);

        if (be.output == null) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    98,
                    18,
                    16,
                    52,
                    List.of(
                            Component.translatable("desc.shared.empty")
                                    .withStyle(ChatFormatting.RED)));
        } else {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    98,
                    18,
                    16,
                    52,
                    List.of(
                            Component.empty()
                                    .append(be.output.material.displayName())
                                    .append(Component.literal(": "))
                                    .append(
                                            MaterialTooltip.of(
                                                    be.output.amount, ModifierKeys.detailsHeld()))
                                    .withStyle(ChatFormatting.YELLOW)));
        }

        if (menu.getCarried().isEmpty()
                && be.getItem(4).isEmpty()
                && isHovering(44, 54, 16, 16, mouseX, mouseY)) {
            List<Component> desc = new ArrayList<>();
            for (String line : RotaryFurnaceBlockEntity.BURN_MODULE.getDesc())
                desc.add(Component.literal(line));
            if (!desc.isEmpty())
                graphics.setComponentTooltipForNextFrame(font, desc, mouseX, mouseY);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private RotaryFurnaceBlockEntity furnace() {
        return menu.blockEntity();
    }
}

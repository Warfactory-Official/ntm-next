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

import com.hbm.blockentity.machine.thermal.SteelFurnaceBlockEntity;
import com.hbm.inventory.container.FurnaceSteelMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FurnaceSteelScreen extends InfoContainerScreen<FurnaceSteelMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_furnace_steel.png");

    public FurnaceSteelScreen(FurnaceSteelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        SteelFurnaceBlockEntity be = menu.blockEntity();

        int h = be.heat * 48 / SteelFurnaceBlockEntity.MAX_HEAT;
        if (h > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    152,
                    67 - h,
                    176,
                    76 - h,
                    7,
                    h,
                    256,
                    256);

        for (int i = 0; i < 3; i++) {
            int p = be.progress[i] * 69 / SteelFurnaceBlockEntity.PROCESS_TIME;
            if (p > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        54,
                        18 + 18 * i,
                        176,
                        18,
                        p,
                        5,
                        256,
                        256);
            int b = Math.min(be.bonus[i], 100) * 69 / 100;
            if (b > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        54,
                        27 + 18 * i,
                        176,
                        23,
                        b,
                        5,
                        256,
                        256);
            if (be.wasOn)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        16,
                        16 + 18 * i,
                        176,
                        0,
                        18,
                        18,
                        256,
                        256);
        }

        for (int i = 0; i < 3; i++) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    53,
                    17 + 18 * i,
                    70,
                    7,
                    List.of(
                            Component.literal(
                                    String.format(Locale.ROOT, "%,d", be.progress[i])
                                            + " / "
                                            + String.format(
                                                    Locale.ROOT,
                                                    "%,d",
                                                    SteelFurnaceBlockEntity.PROCESS_TIME)
                                            + "TU")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    53,
                    26 + 18 * i,
                    70,
                    7,
                    List.of(
                            Component.translatable(
                                    "desc.gui.furnaceSteel.bonus", be.bonus[i] + "%")));
        }
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                151,
                18,
                9,
                50,
                List.of(
                        Component.literal(
                                String.format(Locale.ROOT, "%,d", be.heat)
                                        + " / "
                                        + String.format(
                                                Locale.ROOT,
                                                "%,d",
                                                SteelFurnaceBlockEntity.MAX_HEAT)
                                        + "TU")));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

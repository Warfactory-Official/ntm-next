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

import com.hbm.blockentity.machine.thermal.IronFurnaceBlockEntity;
import com.hbm.inventory.container.FurnaceIronMenu;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FurnaceIronScreen extends InfoContainerScreen<FurnaceIronMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_furnace_iron.png");

    public FurnaceIronScreen(FurnaceIronMenu menu, Inventory inventory, Component title) {
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

        IronFurnaceBlockEntity be = menu.blockEntity();

        int p = be.progress * 70 / Math.max(be.processingTime, 1);
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 53, 36, 176, 18, p, 5, 256, 256);
        int b = be.burnTime * 70 / Math.max(be.maxBurnTime, 1);
        if (b > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 53, 45, 176, 23, b, 5, 256, 256);
        if (be.smeltable)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 70, 16, 176, 0, 18, 18, 256, 256);

        if (menu.getCarried().isEmpty()) {
            if (isHovering(53, 53, 16, 16, mouseX, mouseY)
                    || isHovering(71, 53, 16, 16, mouseX, mouseY)) {
                List<String> desc = be.burnModule.getDesc();
                if (!desc.isEmpty()) {
                    List<Component> lines = new ArrayList<>();
                    for (String s : desc) lines.add(Component.literal(s));
                    graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
                }
            }
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                52,
                35,
                71,
                7,
                List.of(
                        Component.literal(
                                (be.progress * 100 / Math.max(be.processingTime, 1)) + "%")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                52,
                44,
                71,
                7,
                List.of(Component.literal((be.burnTime / SharedConstants.TICKS_PER_SECOND) + "s")));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

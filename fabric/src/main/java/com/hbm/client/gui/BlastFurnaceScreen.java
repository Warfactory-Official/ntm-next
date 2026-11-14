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

import com.hbm.blockentity.machine.BlastFurnaceBlockEntity;
import com.hbm.inventory.container.BlastFurnaceMenu;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class BlastFurnaceScreen extends InfoContainerScreen<BlastFurnaceMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_blast_furnace.png");

    public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.titleLabelY = 6;
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

        BlastFurnaceBlockEntity be = furnace();

        int fuel = (int) Math.round((double) be.fuel * 26D / BlastFurnaceBlockEntity.MAX_FUEL);
        int prog = (int) Math.round(be.progress * (88D - fuel));
        if (prog > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    62,
                    106 - prog - fuel,
                    176,
                    102 - prog - fuel,
                    56,
                    prog,
                    256,
                    256);
        }
        if (fuel > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    62,
                    106 - fuel,
                    176,
                    128 - fuel,
                    56,
                    fuel,
                    256,
                    256);
        }
        if (be.processing) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 81, 64, 176, 0, 14, 14, 256, 256);
        }

        SmoothGaugeElement.draw(
                graphics,
                34,
                80,
                (double) be.tanks[0].getFill() / be.tanks[0].getMaxFill(),
                5,
                2,
                1,
                0x800000,
                0x000000);
        SmoothGaugeElement.draw(
                graphics,
                34,
                26,
                (double) be.tanks[1].getFill() / be.tanks[1].getMaxFill(),
                5,
                2,
                1,
                0x800000,
                0x000000);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                79,
                62,
                18,
                18,
                List.of(Component.translatable("desc.shared.speed", (int) (be.speed * 100) + "%")));
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 25, 71, 18, 18, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 25, 17, 18, 18, be.tanks[1]);

        if (this.menu.getCarried().isEmpty()
                && this.hoveredSlot != null
                && this.hoveredSlot == this.menu.slots.get(BlastFurnaceBlockEntity.SLOT_FUEL)
                && !this.hoveredSlot.hasItem()) {
            List<String> bonuses = be.getFuelBonuses();
            if (!bonuses.isEmpty()) {
                List<Component> lines = new ArrayList<>(bonuses.size());
                for (String s : bonuses) lines.add(Component.literal(s));
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlastFurnaceBlockEntity furnace() {
        return menu.blockEntity();
    }
}

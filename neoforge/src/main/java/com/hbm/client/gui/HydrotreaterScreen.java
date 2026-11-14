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

import com.hbm.blockentity.machine.HydrotreaterBlockEntity;
import com.hbm.inventory.container.HydrotreaterMenu;
import com.hbm.items.ModItems;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class HydrotreaterScreen extends InfoContainerScreen<HydrotreaterMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_hydrotreater.png");

    private static final int POWER_X = 17, TANK_TOP = 18, BAR_W = 16, BAR_H = 52;

    private static final int POWER_FILL_MAX = 54;
    private static final int TANK0_X = 35, TANK1_X = 53, TANK2_X = 125, TANK3_X = 143;
    private static final int CATALYST_X = 89, CATALYST_Y = 36;

    public HydrotreaterScreen(HydrotreaterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 238);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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

        long power = menu.getPower();
        long max = HydrotreaterBlockEntity.MAX_POWER;
        if (power > 0 && max > 0) {
            int filled = (int) Math.min(POWER_FILL_MAX, power * POWER_FILL_MAX / max);
            if (filled > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        TANK_TOP + (BAR_H - filled),
                        176,
                        BAR_H - filled,
                        BAR_W,
                        filled,
                        256,
                        256);
            }
        }

        HydrotreaterBlockEntity be = hydrotreater();
        drawFluidBar(graphics, TANK0_X, TANK_TOP, BAR_W, BAR_H, be.tanks[0]);
        drawFluidBar(graphics, TANK1_X, TANK_TOP, BAR_W, BAR_H, be.tanks[1]);
        drawFluidBar(graphics, TANK2_X, TANK_TOP, BAR_W, BAR_H, be.tanks[2]);
        drawFluidBar(graphics, TANK3_X, TANK_TOP, BAR_W, BAR_H, be.tanks[3]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                TANK_TOP,
                BAR_W,
                BAR_H,
                menu.getPower(),
                HydrotreaterBlockEntity.MAX_POWER);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK0_X, TANK_TOP, BAR_W, BAR_H, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK1_X, TANK_TOP, BAR_W, BAR_H, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK2_X, TANK_TOP, BAR_W, BAR_H, be.tanks[2]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK3_X, TANK_TOP, BAR_W, BAR_H, be.tanks[3]);

        if (menu.getCarried().isEmpty()
                && isHovering(CATALYST_X, CATALYST_Y, 16, 16, mouseX, mouseY)
                && !menu.slots.get(HydrotreaterBlockEntity.SLOT_CATALYST).hasItem()) {
            graphics.setTooltipForNextFrame(
                    this.font, new ItemStack(ModItems.CATALYTIC_CONVERTER), mouseX, mouseY);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private HydrotreaterBlockEntity hydrotreater() {
        return menu.blockEntity();
    }
}

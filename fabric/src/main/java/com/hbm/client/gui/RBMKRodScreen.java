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

import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.inventory.container.RBMKRodMenu;
import com.hbm.items.machine.RBMKRodItem;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class RBMKRodScreen extends InfoContainerScreen<RBMKRodMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/rbmk/gui_rbmk_element.png");
    private static final int PANEL_X = -16,
            AUTOLOADER_PANEL_Y = 20,
            MANUAL_PANEL_Y = 36,
            PANEL_SIZE = 16;

    public RBMKRodScreen(RBMKRodMenu menu, Inventory playerInventory, Component title) {
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

        ItemStack fuel = menu.slots.get(0).getItem();
        if (fuel.getItem() instanceof RBMKRodItem) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 34, 21, 176, 0, 18, 67, 256, 256);
            double depletion = 1D - RBMKRodItem.getEnrichment(fuel);
            int d = (int) (depletion * 67);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 34, 21, 194, 0, 18, d, 256, 256);

            double xenon = RBMKRodItem.getPoisonLevel(fuel);
            int x = (int) (xenon * 58);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    126,
                    82 - x,
                    212,
                    58 - x,
                    14,
                    x,
                    256,
                    256);
        }

        if (!RBMKRodBlockEntity.coldEnoughForAutoloader(fuel)) {
            drawInfoPanel(graphics, PANEL_X, AUTOLOADER_PANEL_Y, PANEL_SIZE, PANEL_SIZE, 6);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    PANEL_X,
                    AUTOLOADER_PANEL_Y,
                    PANEL_SIZE,
                    PANEL_SIZE,
                    PANEL_X + 8,
                    AUTOLOADER_PANEL_Y + PANEL_SIZE,
                    List.of(
                            Component.translatable("desc.gui.rbmkRod.fuelSkinTemperature1000"),
                            Component.translatable("desc.gui.rbmkRod.autoloadersCanNoLonger")));
        }
        if (!RBMKRodBlockEntity.coldEnoughForManual(fuel)) {
            drawInfoPanel(graphics, PANEL_X, MANUAL_PANEL_Y, PANEL_SIZE, PANEL_SIZE, 7);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    PANEL_X,
                    MANUAL_PANEL_Y,
                    PANEL_SIZE,
                    PANEL_SIZE,
                    PANEL_X + 8,
                    MANUAL_PANEL_Y + PANEL_SIZE,
                    List.of(
                            Component.translatable("desc.gui.rbmkRod.fuelSkinTemperature200"),
                            Component.translatable("desc.gui.rbmkRod.fuelCanNoLonger")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

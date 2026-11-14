/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.gui;

import com.hbm.inventory.container.SatDockMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SatDockScreen extends InfoContainerScreen<SatDockMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_sat_dock.png");
    private static final int PANEL_X = -7, PANEL_Y = 36, PANEL_SIZE = 16;

    public SatDockScreen(SatDockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
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
        drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_SIZE, PANEL_SIZE, 2);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y,
                PANEL_SIZE,
                PANEL_SIZE,
                PANEL_X + 8,
                PANEL_Y + PANEL_SIZE,
                lineArray("desc.gui.satdock.desc"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected int titleCenterX() {
        return 115;
    }
}

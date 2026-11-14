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

import com.hbm.inventory.container.SatLinkerMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SatLinkerScreen extends InfoContainerScreen<SatLinkerMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_sat_linker.png");

    private static final int PANEL_X = 12, PANEL_W = 16, PANEL_H = 16;
    private static final int COPY_Y = 28, RANDOMIZE_Y = 44;
    private static final int TOOLTIP_X = 20;

    public SatLinkerScreen(SatLinkerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
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

        drawInfoPanel(graphics, PANEL_X, COPY_Y, PANEL_W, PANEL_H, 2);
        drawInfoPanel(graphics, PANEL_X, RANDOMIZE_Y, PANEL_W, PANEL_H, 3);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                COPY_Y,
                PANEL_W,
                PANEL_H,
                TOOLTIP_X,
                COPY_Y + PANEL_H,
                lineArray("desc.gui.satlinker.chip"));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                RANDOMIZE_Y,
                PANEL_W,
                PANEL_H,
                TOOLTIP_X,
                RANDOMIZE_Y + PANEL_H,
                lineArray("desc.gui.satlinker.random"));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

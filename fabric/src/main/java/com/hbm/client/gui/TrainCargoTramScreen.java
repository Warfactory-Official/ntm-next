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

import com.hbm.inventory.container.TrainCargoTramMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class TrainCargoTramScreen extends InfoContainerScreen<TrainCargoTramMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/vehicles/gui_cargo_tram.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int POWER_BOTTOM = 70, POWER_U = 176, POWER_V_BOTTOM = 52;
    private static final int LIT_X = 156, LIT_Y = 4;

    public TrainCargoTramScreen(TrainCargoTramMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 70;
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

        int fill = menu.getPower() * POWER_H / menu.getMaxPower();
        if (fill > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_BOTTOM - fill,
                    POWER_U,
                    POWER_V_BOTTOM - fill,
                    POWER_W,
                    fill,
                    256,
                    256);
        }

        if (menu.getPower() > menu.getPowerConsumption()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LIT_X,
                    LIT_Y,
                    POWER_U,
                    POWER_V_BOTTOM,
                    9,
                    12,
                    256,
                    256);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                menu.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

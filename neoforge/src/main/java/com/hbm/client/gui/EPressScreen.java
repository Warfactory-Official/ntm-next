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

import com.hbm.blockentity.processing.ElectricPressBlockEntity;
import com.hbm.inventory.container.EPressMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class EPressScreen extends InfoContainerScreen<EPressMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_electric_press.png");

    private static final int POWER_X = 152,
            POWER_BOTTOM = 52,
            POWER_U = 176,
            POWER_W = 16,
            POWER_H = 34;
    private static final int BAR_X = 18,
            BAR_Y = 33,
            BAR_U = 192,
            BAR_V = 0,
            BAR_W = 18,
            BAR_MAX_H = 16;

    public EPressScreen(EPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 89;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ElectricPressBlockEntity press = menu.blockEntity();

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

        int filled = (int) (press.power * POWER_H / ElectricPressBlockEntity.MAX_POWER);
        if (filled > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_BOTTOM - filled,
                    POWER_U,
                    POWER_H - filled,
                    POWER_W,
                    filled,
                    256,
                    256);
        }

        int h = (int) (press.renderPress * BAR_MAX_H / ElectricPressBlockEntity.MAX_PROGRESS);
        if (h > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    BAR_X,
                    BAR_Y,
                    BAR_U,
                    BAR_V,
                    BAR_W,
                    h,
                    256,
                    256);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_BOTTOM - POWER_H,
                POWER_W,
                POWER_H,
                press.power,
                ElectricPressBlockEntity.MAX_POWER);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

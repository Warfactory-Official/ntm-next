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

import com.hbm.blockentity.machine.StorageDrumBlockEntity;
import com.hbm.inventory.container.StorageDrumMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class StorageDrumScreen extends InfoContainerScreen<StorageDrumMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_drum.png");

    private static final int BAR_HEIGHT = 106;
    private static final int BAR_WIDTH = 7;
    private static final int BAR_BOTTOM = 130;

    public StorageDrumScreen(StorageDrumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 234);
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

        StorageDrumBlockEntity be = menu.blockEntity();

        drawBar(graphics, be.tank(0).getFill(), be.tank(0).getMaxFill(), 17, 176);
        drawBar(graphics, be.tank(1).getFill(), be.tank(1).getMaxFill(), 152, 183);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 16, 23, 9, 108, be.tank(0));
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 151, 23, 9, 108, be.tank(1));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private static void drawBar(GuiGraphicsExtractor graphics, int fill, int max, int x, int u) {
        int height = fill * BAR_HEIGHT / max;
        if (height <= 0) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                BAR_BOTTOM - height,
                u,
                BAR_HEIGHT - height,
                BAR_WIDTH,
                height,
                256,
                256);
    }
}

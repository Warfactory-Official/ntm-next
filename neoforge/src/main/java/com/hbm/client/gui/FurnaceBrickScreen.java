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

import com.hbm.blockentity.machine.thermal.BrickFurnaceBlockEntity;
import com.hbm.inventory.container.FurnaceBrickMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class FurnaceBrickScreen extends InfoContainerScreen<FurnaceBrickMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_furnace_brick.png");

    public FurnaceBrickScreen(FurnaceBrickMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
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

        BrickFurnaceBlockEntity be = menu.blockEntity();

        if (be.burnTime > 0) {
            int b = be.burnTime * 13 / Math.max(be.maxBurnTime, 1);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    62,
                    54 + 12 - b,
                    176,
                    12 - b,
                    14,
                    b + 1,
                    256,
                    256);
            int p = be.progress * 24 / BrickFurnaceBlockEntity.SMELT_TIME;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 85, 34, 176, 14, p + 1, 16, 256, 256);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

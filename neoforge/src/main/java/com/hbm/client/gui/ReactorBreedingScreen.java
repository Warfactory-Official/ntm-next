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

import com.hbm.blockentity.machine.ReactorBreedingBlockEntity;
import com.hbm.inventory.container.ReactorBreedingMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ReactorBreedingScreen extends InfoContainerScreen<ReactorBreedingMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_breeder.png");

    private static final int ARROW_X = 53, ARROW_Y = 32, ARROW_W = 70, ARROW_H = 20;
    private static final int PANEL_X = -16, PANEL_Y = 16, PANEL_W = 16, PANEL_H = 16;

    public ReactorBreedingScreen(ReactorBreedingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
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

        ReactorBreedingBlockEntity be = menu.blockEntity();

        int progress = (int) (be.progress * ARROW_W);
        if (progress > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ARROW_X,
                    ARROW_Y,
                    176.0F,
                    0.0F,
                    progress,
                    ARROW_H,
                    256,
                    256);
        }

        drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 3);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y,
                PANEL_W,
                PANEL_H,
                PANEL_X + 8,
                PANEL_Y + 32,
                List.of(
                        Component.translatable("desc.gui.machineReactorBreeding.theReactorHasTo"),
                        Component.translatable(
                                "desc.gui.machineReactorBreeding.neutronFluxFromAdjacent"),
                        Component.translatable(
                                "desc.gui.machineReactorBreeding.researchReactorsToBreed")));

        String flux = String.valueOf(menu.getFlux());
        graphics.text(this.font, flux, 88 - this.font.width(flux) / 2, 21, 0xFF08FF00, false);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

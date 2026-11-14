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

import com.hbm.blockentity.machine.FusionPelletPressBlockEntity;
import com.hbm.inventory.container.ICFPressMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ICFPressScreen extends InfoContainerScreen<ICFPressMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_icf_press.png");

    public ICFPressScreen(ICFPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 179);
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

        FusionPelletPressBlockEntity press = menu.blockEntity();

        int m = press.charge() * 52 / FusionPelletPressBlockEntity.CHARGE_PER_MUON;
        if (m > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 28, 70 - m, 176, 52 - m, 4, m, 256, 256);

        drawFluidBar(graphics, 44, 18, 16, 52, press.tank(0));
        drawFluidBar(graphics, 152, 18, 16, 52, press.tank(1));

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 44, 18, 16, 52, press.tank(0));
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 152, 18, 16, 52, press.tank(1));

        if (!menu.getSlot(4).hasItem()) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    menu.getSlot(4).x,
                    menu.getSlot(4).y,
                    16,
                    16,
                    List.of(
                            Component.translatable("desc.gui.icfPress.itemInputTopBottom")
                                    .withStyle(ChatFormatting.YELLOW)));
        }
        if (!menu.getSlot(5).hasItem()) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    menu.getSlot(5).x,
                    menu.getSlot(5).y,
                    16,
                    16,
                    List.of(
                            Component.translatable("desc.gui.icfPress.itemInputSides")
                                    .withStyle(ChatFormatting.YELLOW)));
        }
        super.extractLabels(graphics, mouseX, mouseY);
    }
}

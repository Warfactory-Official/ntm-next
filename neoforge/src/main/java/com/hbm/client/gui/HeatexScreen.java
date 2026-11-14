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

import com.hbm.blockentity.machine.thermal.CoolingHeatBlockEntity;
import com.hbm.inventory.container.HeatexMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.HeatexCoolAmountLayout;
import com.hbm.packet.layout.HeatexDelayLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class HeatexScreen extends InfoContainerScreen<HeatexMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_heatex.png");

    private static final int TANK0_X = 44, TANK1_X = 116, TANK_Y = 36, TANK_W = 16, TANK_H = 52;

    private EditBox toCoolField;
    private EditBox delayField;

    public HeatexScreen(HeatexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        CoolingHeatBlockEntity be = heatex();

        toCoolField = new EditBox(this.font, leftPos + 73, topPos + 31, 30, 10, Component.empty());
        toCoolField.setBordered(false);
        toCoolField.setMaxLength(6);
        toCoolField.setTextColor(CommonColors.GREEN);
        toCoolField.setValue(String.valueOf(be.amountToCool));
        toCoolField.setResponder(text -> sendControl("toCool", text));
        addRenderableWidget(toCoolField);

        delayField = new EditBox(this.font, leftPos + 73, topPos + 49, 30, 10, Component.empty());
        delayField.setBordered(false);
        delayField.setMaxLength(6);
        delayField.setTextColor(CommonColors.GREEN);
        delayField.setValue(String.valueOf(be.tickDelay));
        delayField.setResponder(text -> sendControl("delay", text));
        addRenderableWidget(delayField);
    }

    private void sendControl(String key, String text) {
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return;
        }
        if (key.equals("toCool"))
            PacketWire.sendToServer(HeatexCoolAmountLayout.encode(menu.containerId, value));
        else if (key.equals("delay"))
            PacketWire.sendToServer(HeatexDelayLayout.encode(menu.containerId, value));
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        CoolingHeatBlockEntity be = heatex();

        drawFluidBar(graphics, TANK0_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidBar(graphics, TANK1_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK0_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK1_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);

        if (checkClick(mouseX, mouseY, 70, 26, 36, 18)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    70,
                    26,
                    36,
                    18,
                    List.of(Component.translatable("desc.gui.machineHeatex.amountPerCycle")));
        }
        if (checkClick(mouseX, mouseY, 70, 44, 36, 18)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    70,
                    44,
                    36,
                    18,
                    List.of(Component.translatable("desc.gui.machineHeatex.cycleTickDelay")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private CoolingHeatBlockEntity heatex() {
        return menu.blockEntity();
    }
}

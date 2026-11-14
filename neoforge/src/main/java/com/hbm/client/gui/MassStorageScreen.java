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

import com.hbm.blockentity.machine.storage.MassStorageBlockEntity;
import com.hbm.client.Keybinds;
import com.hbm.inventory.container.MassStorageMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MassStorageOutputLayout;
import com.hbm.packet.layout.MassStorageProvideLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class MassStorageScreen extends InfoContainerScreen<MassStorageMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_mass_storage.png");

    private static final int GAUGE_X = 97, GAUGE_Y = 105, GAUGE_W = 16, GAUGE_H = 88;
    private static final int INFO_X = 96, INFO_Y = 16, INFO_W = 18, INFO_H = 90;
    private static final int PROVIDE_X = 62, TOGGLE_X = 80, BUTTON_Y = 72, BUTTON_SIZE = 14;

    public MassStorageScreen(MassStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 221);
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

        MassStorageBlockEntity be = menu.blockEntity();
        int stockpile = be.getStockpile();
        int capacity = be.getCapacity();

        int gauge = stockpile * GAUGE_H / capacity;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                GAUGE_X,
                GAUGE_Y - gauge,
                176,
                GAUGE_H - gauge,
                GAUGE_W,
                gauge,
                256,
                256);
        if (be.isOutput()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    BUTTON_Y,
                    192,
                    0,
                    BUTTON_SIZE,
                    BUTTON_SIZE,
                    256,
                    256);
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y,
                INFO_W,
                INFO_H,
                List.of(
                        Component.literal(
                                String.format(Locale.ROOT, "%,d / %,d", stockpile, capacity)),
                        Component.literal((stockpile * 1000L / capacity) / 10D + "%")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PROVIDE_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(
                        Component.translatable("desc.gui.massStorage.clickProvideOne"),
                        Component.translatable(
                                "tooltip.hbm.mass_storage.provide_stack",
                                Keybinds.BATCH.getTranslatedKeyMessage())));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                TOGGLE_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(Component.translatable("desc.gui.massStorage.toggleOutput")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, PROVIDE_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                PacketWire.sendToServer(
                        MassStorageProvideLayout.encode(menu.containerId, Keybinds.BATCH.isDown()));
                playClick();
                return true;
            }
            if (checkClick(mx, my, TOGGLE_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                PacketWire.sendToServer(MassStorageOutputLayout.encode(menu.containerId));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

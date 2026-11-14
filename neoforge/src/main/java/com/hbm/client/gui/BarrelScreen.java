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

import com.hbm.blockentity.machine.storage.FluidBarrelBlockEntity;
import com.hbm.inventory.container.BarrelMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.BarrelModeLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class BarrelScreen extends InfoContainerScreen<BarrelMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_barrel.png");

    private static final String[] MODE_KEYS = {
        "desc.shared.tankMode.inputOnly",
        "desc.shared.tankMode.buffer",
        "desc.shared.tankMode.outputOnly",
        "desc.shared.disabled"
    };

    private static final int BAR_X = 71, BAR_Y = 17, BAR_W = 34, BAR_H = 52;
    private static final int BTN_X = 151, BTN_Y = 34, BTN_W = 18, BTN_H = 18;
    private static final int BTN_HIT_Y = 35;

    public BarrelScreen(BarrelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
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

        FluidBarrelBlockEntity be = menu.blockEntity();
        drawFluidBar(graphics, BAR_X, BAR_Y, BAR_W, BAR_H, be.tank);
        int mode = be.getMode().code();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                BTN_X,
                BTN_Y,
                176,
                mode * BTN_H,
                BTN_W,
                BTN_H,
                256,
                256);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, BAR_X, BAR_Y, BAR_W, BAR_H, be.tank);
        if (checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    BTN_X,
                    BTN_Y,
                    BTN_W,
                    BTN_H,
                    List.of(
                            Component.translatable(
                                    "desc.shared.mode", Component.translatable(MODE_KEYS[mode]))));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, BTN_X, BTN_HIT_Y, BTN_W, BTN_H)) {
                PacketWire.sendToServer(BarrelModeLayout.encode(menu.containerId));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

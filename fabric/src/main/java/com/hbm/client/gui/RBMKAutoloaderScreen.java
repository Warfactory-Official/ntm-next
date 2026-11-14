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

import com.hbm.inventory.container.RBMKAutoloaderMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RBMKAutoloaderStepLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class RBMKAutoloaderScreen extends InfoContainerScreen<RBMKAutoloaderMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_autoloader.png");
    private static final int BUTTON_SIZE = 12;
    private static final int MINUS_X = 74;
    private static final int PLUS_X = 90;
    private static final int BUTTON_Y = 36;

    public RBMKAutoloaderScreen(
            RBMKAutoloaderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 182);
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

        Component percent = Component.literal(menu.blockEntity().cycle + "%");
        graphics.text(
                font, percent, imageWidth / 2 - font.width(percent) / 2, 23, 0xFF00FF00, false);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int x = (int) event.x(), y = (int) event.y();
            if (checkClick(x, y, MINUS_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) return send(false);
            if (checkClick(x, y, PLUS_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) return send(true);
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean send(boolean increase) {
        PacketWire.sendToServer(RBMKAutoloaderStepLayout.encode(menu.containerId, increase));
        playClick();
        return true;
    }
}

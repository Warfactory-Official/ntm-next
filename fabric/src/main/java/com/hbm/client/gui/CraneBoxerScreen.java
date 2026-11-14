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

import com.hbm.inventory.container.CraneBoxerMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.CraneBoxerCycleLayout;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class CraneBoxerScreen extends InfoContainerScreen<CraneBoxerMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_crane_boxer.png");

    private static final int TOGGLE_X = 151;
    private static final int TOGGLE_Y = 34;
    private static final int TOGGLE_SIZE = 18;

    public CraneBoxerScreen(CraneBoxerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
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
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                TOGGLE_X,
                TOGGLE_Y,
                176,
                menu.blockEntity().mode * 18,
                TOGGLE_SIZE,
                TOGGLE_SIZE,
                256,
                256);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (checkClick(
                (int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, TOGGLE_SIZE, TOGGLE_SIZE)) {
            PacketWire.sendToServer(CraneBoxerCycleLayout.encode(menu.containerId));
            playClick();
        }

        return super.mouseClicked(event, doubleClick);
    }
}

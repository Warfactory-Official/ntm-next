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

import com.hbm.blockentity.network.DroneCrateBlockEntity;
import com.hbm.inventory.container.DroneCrateMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.DroneCrateItemTypeLayout;
import com.hbm.packet.layout.DroneCrateSendingModeLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class DroneCrateScreen extends InfoContainerScreen<DroneCrateMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_crate_drone.png");

    private static final int TANK_X = 125, TANK_Y = 17, TANK_W = 16, TANK_H = 34;
    private static final int BUTTON = 18;
    private static final int TYPE_X = 151, TYPE_Y = 16;
    private static final int MODE_X = 151, MODE_Y = 52;

    public DroneCrateScreen(DroneCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        DroneCrateBlockEntity crate = menu.blockEntity();

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
                TYPE_X,
                TYPE_Y,
                194,
                crate.itemCargo ? 0 : BUTTON,
                BUTTON,
                BUTTON,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                MODE_X,
                MODE_Y,
                176,
                crate.sending ? BUTTON : 0,
                BUTTON,
                BUTTON,
                256,
                256);

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, crate.tank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, crate.tank);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, TYPE_X, TYPE_Y, BUTTON, BUTTON)) return toggle("type");
            if (checkClick(mx, my, MODE_X, MODE_Y, BUTTON, BUTTON)) return toggle("mode");
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean toggle(String key) {
        if (key.equals("type")) {
            PacketWire.sendToServer(DroneCrateItemTypeLayout.encode(menu.containerId));
        } else {
            PacketWire.sendToServer(DroneCrateSendingModeLayout.encode(menu.containerId));
        }
        playClick();
        return true;
    }
}

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

import com.hbm.blockentity.network.CraneGrabberBlockEntity;
import com.hbm.inventory.container.CraneGrabberMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.CraneGrabberWhitelistLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class CraneGrabberScreen extends InfoContainerScreen<CraneGrabberMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_crane_grabber.png");

    private static final int LEVER_X = 97;
    private static final int LEVER_Y = 30;
    private static final int LEVER_W = 14;
    private static final int LEVER_H = 26;

    public CraneGrabberScreen(CraneGrabberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        CraneGrabberBlockEntity grabber = menu.blockEntity();

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
                108,
                grabber.isWhitelist ? 33 : 47,
                176,
                0,
                3,
                6,
                256,
                256);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> slotTooltip(Slot slot) {
        return slot.index < CraneGrabberBlockEntity.FILTER_SLOTS
                ? patternModeLines(menu.blockEntity().matcher.mode(slot.index))
                : List.of();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (checkClick((int) event.x(), (int) event.y(), LEVER_X, LEVER_Y, LEVER_W, LEVER_H)) {
            PacketWire.sendToServer(CraneGrabberWhitelistLayout.encode(menu.containerId));
            playClick();
        }

        return super.mouseClicked(event, doubleClick);
    }
}

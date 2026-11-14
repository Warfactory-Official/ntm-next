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

import com.hbm.blockentity.machine.RadarBlockEntity;
import com.hbm.inventory.container.RadarSlotsMenu;
import com.hbm.main.Polaroid;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RadarOpenMapLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class RadarSlotsScreen extends InfoContainerScreen<RadarSlotsMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_radar_link.png");
    private static final Component POLAROID_TITLE =
            Component.translatable("desc.gui.machineRadarSlots.reda");

    public RadarSlotsScreen(RadarSlotsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        RadarBlockEntity be = menu.blockEntity();

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

        if (be.power > 0) {
            int i = (int) (be.power * 160 / be.getMaxPower());
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 64, 0.0F, 185.0F, i, 16, 256, 256);
        }

        drawCustomInfoStat(graphics, mouseX, mouseY, 5, 5, 8, 8, lineArray("radar.toggleGui"));

        if (Polaroid.isBalefireDay()) {
            int tx = titleCenterX() - font.width(POLAROID_TITLE) / 2;
            graphics.text(font, POLAROID_TITLE, tx, titleLabelY, titleColor(), false);
            graphics.text(
                    font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, -12566464, false);
            return;
        }
        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), 5, 5, 8, 8)) {
            playClick();
            PacketWire.sendToServer(RadarOpenMapLayout.encode(menu.containerId));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}

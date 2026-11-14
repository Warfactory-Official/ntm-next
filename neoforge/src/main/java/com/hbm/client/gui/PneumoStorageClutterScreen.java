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

import com.hbm.blockentity.network.pneumatic.PneumaticStorageClutterBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticTubeBlockEntity;
import com.hbm.inventory.container.PneumoStorageClutterMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PneumaticPressureLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class PneumoStorageClutterScreen extends InfoContainerScreen<PneumoStorageClutterMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_pneumatic_clutter.png");

    public PneumoStorageClutterScreen(
            PneumoStorageClutterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 235);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 176 / 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PneumaticStorageClutterBlockEntity storage = menu.blockEntity();

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
                174 + 4 * (storage.airTank.getPressure() - 1),
                36,
                200,
                0,
                4,
                8,
                256,
                256);
        SmoothGaugeElement.draw(
                graphics,
                184,
                25,
                (double) storage.airTank.getFill() / storage.airTank.getMaxFill(),
                5,
                2,
                1,
                0xFFCA6C43,
                0xFFAB4223);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                174,
                36,
                20,
                8,
                List.of(
                        Component.translatable(
                                "desc.shared.compressor", storage.airTank.getPressure()),
                        Component.translatable(
                                "desc.shared.maxRange",
                                PneumaticTubeBlockEntity.rangeForPressure(
                                        storage.airTank.getPressure()))));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        if (checkClick((int) x, (int) y, 174, 36, 20, 8)) {
            PacketWire.sendToServer(PneumaticPressureLayout.encode(menu.containerId));
            playClick();
        }
        return super.mouseClicked(event, doubleClick);
    }
}

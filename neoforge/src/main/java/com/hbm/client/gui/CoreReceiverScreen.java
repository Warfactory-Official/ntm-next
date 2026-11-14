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

import com.hbm.blockentity.machine.dfc.DfcReceiverBlockEntity;
import com.hbm.inventory.container.CoreReceiverMenu;
import com.hbm.util.NumberFormatter;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class CoreReceiverScreen extends InfoContainerScreen<CoreReceiverMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/dfc/gui_receiver.png");

    private static final int GAUGE_W = 16, GAUGE_H = 52, GAUGE_Y = 17, TANK_X = 8;
    private static final int TEXT_COLOR = 0xFFFF7F7F;

    public CoreReceiverScreen(CoreReceiverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
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
        DfcReceiverBlockEntity be = menu.blockEntity();

        drawFluidBar(graphics, TANK_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tank);

        graphics.text(
                font,
                Component.translatable("desc.gui.coreReceiver.input"),
                40,
                25,
                TEXT_COLOR,
                false);
        graphics.text(
                font,
                Component.literal(NumberFormatter.formatCompact(be.joules) + "Spk"),
                50,
                35,
                TEXT_COLOR,
                false);
        graphics.text(
                font,
                Component.translatable("desc.gui.coreReceiver.output"),
                40,
                45,
                TEXT_COLOR,
                false);
        graphics.text(
                font,
                Component.literal(
                        NumberFormatter.formatCompact(be.joules * DfcReceiverBlockEntity.HE_PER_SPK)
                                + "HE"),
                50,
                55,
                TEXT_COLOR,
                false);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tank);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

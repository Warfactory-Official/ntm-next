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

import com.hbm.blockentity.network.FluidPumpBlockEntity;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.FluidPumpSettingsLayout;
import com.hbm.uninos.ConnectionPriority;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FluidPumpScreen extends MenuScreen<ScreenMenu<FluidPumpBlockEntity>> {

    private final FluidPumpBlockEntity pump;
    private EditBox throughput;
    private int pressure;
    private int priority;

    public FluidPumpScreen(
            ScreenMenu<FluidPumpBlockEntity> menu, Inventory inventory, Component title) {
        super(menu, title);
        this.pump = menu.blockEntity();
        this.pressure = pump.pressure;
        this.priority = pump.priority.ordinal();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        throughput =
                new EditBox(
                        this.font,
                        cx - 150,
                        100,
                        90,
                        20,
                        Component.translatable("gui.pump.throughput"));
        throughput.setMaxLength(5);
        throughput.setValue(Integer.toString(pump.rate));
        addRenderableWidget(throughput);

        addRenderableWidget(
                Button.builder(
                                Component.literal(pressure + " PU"),
                                b -> {
                                    pressure = (pressure + 1) % 6;
                                    b.setMessage(Component.literal(pressure + " PU"));
                                })
                        .bounds(cx - 50, 100, 90, 20)
                        .build());

        addRenderableWidget(
                Button.builder(
                                Component.literal(ConnectionPriority.VALUES[priority].name()),
                                b -> {
                                    priority = (priority + 1) % ConnectionPriority.VALUES.length;
                                    b.setMessage(
                                            Component.literal(
                                                    ConnectionPriority.VALUES[priority].name()));
                                })
                        .bounds(cx + 50, 100, 90, 20)
                        .build());

        setInitialFocus(throughput);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;

        graphics.text(
                this.font, Component.translatable("gui.pump.throughput"), cx - 150, 80, 0xFFA0A0A0);
        graphics.text(
                this.font,
                Component.translatable("gui.pump.throughputMax"),
                cx - 150,
                90,
                0xFFA0A0A0);
        graphics.text(
                this.font, Component.translatable("gui.pump.pressure"), cx - 50, 80, 0xFFA0A0A0);
        graphics.text(
                this.font, Component.translatable("gui.pump.priority"), cx + 50, 80, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        boolean ratePresent = false;
        int rate = 0;
        try {
            rate = Integer.parseInt(throughput.getValue().trim());
            ratePresent = true;
        } catch (NumberFormatException ignored) {
        }
        PacketWire.sendToServer(
                FluidPumpSettingsLayout.encode(
                        menu.containerId,
                        ratePresent,
                        rate,
                        true,
                        pressure,
                        true,
                        ConnectionPriority.VALUES[priority]));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

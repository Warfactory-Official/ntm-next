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

import com.hbm.blockentity.network.CableDiodeBlockEntity;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.CableDiodeSettingsLayout;
import com.hbm.uninos.ConnectionPriority;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CableDiodeScreen extends MenuScreen<ScreenMenu<CableDiodeBlockEntity>> {

    private final CableDiodeBlockEntity diode;
    private EditBox throughput;
    private int priority;

    public CableDiodeScreen(
            ScreenMenu<CableDiodeBlockEntity> menu, Inventory inventory, Component title) {
        super(menu, title);
        this.diode = menu.blockEntity();
        this.priority = diode.receiverPriority().ordinal();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        throughput =
                new EditBox(
                        font,
                        cx - 150,
                        100,
                        90,
                        20,
                        Component.translatable("gui.diode.throughput"));
        throughput.setMaxLength(11);
        throughput.setValue(Long.toString(diode.throughputLimit()));
        addRenderableWidget(throughput);
        addRenderableWidget(
                Button.builder(
                                Component.literal(ConnectionPriority.VALUES[priority].name()),
                                button -> {
                                    priority = (priority + 1) % ConnectionPriority.VALUES.length;
                                    button.setMessage(
                                            Component.literal(
                                                    ConnectionPriority.VALUES[priority].name()));
                                })
                        .bounds(cx + 20, 100, 90, 20)
                        .build());
        setInitialFocus(throughput);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        graphics.text(
                font,
                Component.translatable("gui.diode.throughput"),
                cx - 150,
                80,
                0xFFA0A0A0,
                false);
        graphics.text(
                font,
                Component.translatable("gui.diode.throughputMax"),
                cx - 150,
                90,
                0xFFA0A0A0,
                false);
        graphics.text(
                font, Component.translatable("gui.diode.priority"), cx + 20, 80, 0xFFA0A0A0, false);
    }

    @Override
    public void onClose() {
        boolean limitPresent = false;
        long limit = 0;
        try {
            limit = Long.parseLong(throughput.getValue());
            limitPresent = true;
        } catch (NumberFormatException _) {
        }
        PacketWire.sendToServer(
                CableDiodeSettingsLayout.encode(
                        menu.containerId,
                        limitPresent,
                        limit,
                        true,
                        ConnectionPriority.VALUES[priority]));
        super.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!event.isEscape() && throughput.canConsumeInput()) {
            throughput.keyPressed(event);
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

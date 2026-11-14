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

import com.hbm.blockentity.machine.radio.RadioReceiverBlockEntity;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RadioReceiverSettingsLayout;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class RadioRecScreen extends MenuScreen<ScreenMenu<RadioReceiverBlockEntity>> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/machine/gui_radio.png");

    private static final int X_SIZE = 220;
    private static final int Y_SIZE = 42;
    private static final int SAVE_X = 137;
    private static final int TOGGLE_X = 173;
    private static final int BUTTON_Y = 17;
    private static final int BUTTON_SIZE = 18;

    private final RadioReceiverBlockEntity radio;
    private int guiLeft;
    private int guiTop;
    private EditBox frequency;

    public RadioRecScreen(
            ScreenMenu<RadioReceiverBlockEntity> menu, Inventory inventory, Component title) {
        super(menu, title);
        this.radio = menu.blockEntity();
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - X_SIZE) / 2;
        this.guiTop = (this.height - Y_SIZE) / 2;

        frequency =
                new EditBox(
                        this.font,
                        guiLeft + 25 + 4,
                        guiTop + 17 + 4,
                        90 - 8,
                        14,
                        Component.translatable("container.radiorec"));
        frequency.setTextColor(0xFF00FF00);
        frequency.setTextColorUneditable(0xFF00FF00);
        frequency.setBordered(false);
        frequency.setMaxLength(10);
        frequency.setValue(radio.channel);
        addRenderableWidget(frequency);
        setInitialFocus(frequency);
    }

    private boolean inZone(int zoneX, double mouseX, double mouseY) {
        return guiLeft + zoneX <= mouseX
                && guiLeft + zoneX + BUTTON_SIZE > mouseX
                && guiTop + BUTTON_Y < mouseY
                && guiTop + BUTTON_Y + BUTTON_SIZE >= mouseY;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                guiLeft,
                guiTop,
                0.0F,
                0.0F,
                X_SIZE,
                Y_SIZE,
                256,
                256);
        if (radio.enabled) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    guiLeft + TOGGLE_X,
                    guiTop + BUTTON_Y,
                    0.0F,
                    42.0F,
                    BUTTON_SIZE,
                    BUTTON_SIZE,
                    256,
                    256);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Component name = Component.translatable("container.radiorec");
        graphics.text(
                this.font,
                name,
                guiLeft + X_SIZE / 2 - this.font.width(name) / 2,
                guiTop + 6,
                0xFF404040,
                false);

        if (inZone(SAVE_X, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                    this.font, Component.translatable("desc.shared.saveSettings"), mouseX, mouseY);
        }
        if (inZone(TOGGLE_X, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                    this.font, Component.translatable("desc.gui.radioRec.toggle"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();

        if (inZone(SAVE_X, x, y)) {
            playButtonSound();
            PacketWire.sendToServer(
                    RadioReceiverSettingsLayout.encode(
                            menu.containerId, true, frequency.getValue(), false, false));
            return true;
        }

        if (inZone(TOGGLE_X, x, y)) {
            playButtonSound();
            PacketWire.sendToServer(
                    RadioReceiverSettingsLayout.encode(
                            menu.containerId, false, "", true, !radio.enabled));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

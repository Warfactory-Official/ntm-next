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

import com.hbm.blockentity.network.RadioCounterBlockEntity;
import com.hbm.inventory.container.RadioTorchCounterMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RadioTorchCounterChannelsLayout;
import com.hbm.packet.layout.RadioTorchCounterPollingLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class RadioTorchCounterScreen extends InfoContainerScreen<RadioTorchCounterMenu>
        implements RadioTorchCounterChannelsLayout.Channels {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_rtty_counter.png");
    private static final int POLLING_X = 193;
    private static final int POLLING_Y = 8;
    private static final int SAVE_Y = 30;
    private static final int BUTTON_SIZE = 18;

    private final EditBox[] channels = new EditBox[3];

    public RadioTorchCounterScreen(
            RadioTorchCounterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 218, 238);
        inventoryLabelX = 16;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    private static void click() {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < channels.length; i++) {
            EditBox field =
                    new EditBox(
                            font, leftPos + 29, topPos + 21 + 44 * i, 86, 14, Component.empty());
            field.setBordered(false);
            field.setTextColor(0xFF00FF00);
            field.setTextColorUneditable(0xFF00FF00);
            field.setMaxLength(10);
            field.setValue(menu.blockEntity().channels[i]);
            channels[i] = field;
            addRenderableWidget(field);
        }
    }

    @Override
    protected int titleCenterX() {
        return 92;
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
                0F,
                0F,
                imageWidth,
                imageHeight,
                256,
                256);
        if (menu.blockEntity().polling) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + POLLING_X,
                    topPos + POLLING_Y,
                    218F,
                    0F,
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
        if (inButton(mouseX, mouseY, POLLING_Y)) {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.literal(menu.blockEntity().polling ? "Polling" : "State Change"),
                    mouseX,
                    mouseY);
        } else if (inButton(mouseX, mouseY, SAVE_Y)) {
            graphics.setTooltipForNextFrame(
                    font, Component.translatable("desc.shared.saveSettings"), mouseX, mouseY);
        }
    }

    @Override
    protected List<Component> slotTooltip(Slot slot) {
        return slot.index < RadioCounterBlockEntity.FILTER_COUNT
                ? patternModeLines(menu.blockEntity().matcher.mode(slot.index))
                : List.of();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inButton(event.x(), event.y(), POLLING_Y)) {
            PacketWire.sendToServer(RadioTorchCounterPollingLayout.encode(menu.containerId));
            click();
            return true;
        }
        if (inButton(event.x(), event.y(), SAVE_Y)) {
            PacketWire.sendToServer(RadioTorchCounterChannelsLayout.encode(menu.containerId, this));
            click();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public String channel(int index) {
        return channels[index].getValue();
    }

    private boolean inButton(double mouseX, double mouseY, int y) {
        return leftPos + POLLING_X <= mouseX
                && leftPos + POLLING_X + BUTTON_SIZE > mouseX
                && topPos + y < mouseY
                && topPos + y + BUTTON_SIZE >= mouseY;
    }
}

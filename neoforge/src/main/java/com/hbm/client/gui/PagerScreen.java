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

import com.hbm.inventory.container.HeldItemMenu;
import com.hbm.items.tool.RTTYPagerItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RTTYPagerChannelLayout;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public final class PagerScreen extends MenuScreen<HeldItemMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_rtty_pager.png");
    private static final int X_SIZE = 184;
    private static final int Y_SIZE = 42;
    private static final int SAVE_X = 137;
    private static final int BUTTON_Y = 17;
    private static final int BUTTON_SIZE = 18;

    private final String startingChannel;
    private int guiLeft;
    private int guiTop;
    private EditBox channel;

    public PagerScreen(HeldItemMenu menu, Inventory inventory, Component title) {
        super(menu, title);
        startingChannel = RTTYPagerItem.channel(menu.stack());
    }

    @Override
    protected void init() {
        guiLeft = (width - X_SIZE) / 2;
        guiTop = (height - Y_SIZE) / 2;

        channel =
                new EditBox(
                        font,
                        guiLeft + 27 + 4,
                        guiTop + 19 + 4,
                        90 - 8,
                        14,
                        Component.translatable("container.rttyPager"));
        channel.setTextColor(0xFF00FF00);
        channel.setTextColorUneditable(0xFF00FF00);
        channel.setBordered(false);
        channel.setMaxLength(RTTYPagerItem.MAX_CHANNEL_LENGTH);
        channel.setValue(startingChannel);
        addRenderableWidget(channel);
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
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Component title = Component.translatable("container.rttyPager");
        graphics.text(
                font,
                title,
                guiLeft + X_SIZE / 2 - font.width(title) / 2,
                guiTop + 6,
                0xFF404040,
                false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inSaveZone(event.x(), event.y())) {
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            PacketWire.sendToServer(
                    RTTYPagerChannelLayout.encode(menu.containerId, channel.getValue()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean inSaveZone(double x, double y) {
        return guiLeft + SAVE_X <= x
                && guiLeft + SAVE_X + BUTTON_SIZE > x
                && guiTop + BUTTON_Y < y
                && guiTop + BUTTON_Y + BUTTON_SIZE >= y;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.minecraft != null
                && this.minecraft.options.keyInventory.matches(event)
                && !(getFocused() instanceof EditBox box && box.canConsumeInput())) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

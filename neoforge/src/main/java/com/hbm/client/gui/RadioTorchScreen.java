/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.gui;

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioInfoProvider;
import com.hbm.blockentity.network.RadioLogicBlockEntity;
import com.hbm.blockentity.network.RadioReaderBlockEntity;
import com.hbm.blockentity.network.RadioTorchBlockEntity;
import com.hbm.blockentity.network.RadioTorchControllerBlockEntity;
import com.hbm.blockentity.network.RadioTorchReceiverBlockEntity;
import com.hbm.blockentity.network.RadioTorchSenderBlockEntity;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RadioTorchControllerPollingLayout;
import com.hbm.packet.layout.RadioTorchControllerSettingsLayout;
import com.hbm.packet.layout.RadioTorchLogicDescendingLayout;
import com.hbm.packet.layout.RadioTorchLogicPollingLayout;
import com.hbm.packet.layout.RadioTorchLogicSettingsLayout;
import com.hbm.packet.layout.RadioTorchReaderPollingLayout;
import com.hbm.packet.layout.RadioTorchReaderSettingsLayout;
import com.hbm.packet.layout.RadioTorchReceiverCustomMapLayout;
import com.hbm.packet.layout.RadioTorchReceiverPollingLayout;
import com.hbm.packet.layout.RadioTorchReceiverSettingsLayout;
import com.hbm.packet.layout.RadioTorchSenderCustomMapLayout;
import com.hbm.packet.layout.RadioTorchSenderPollingLayout;
import com.hbm.packet.layout.RadioTorchSenderSettingsLayout;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
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
import org.jspecify.annotations.Nullable;

public final class RadioTorchScreen extends MenuScreen<ScreenMenu<RadioTorchBlockEntity>>
        implements RadioTorchSenderSettingsLayout.Mapping,
                RadioTorchReceiverSettingsLayout.Mapping,
                RadioTorchLogicSettingsLayout.Entries,
                RadioTorchReaderSettingsLayout.Lines {

    private static final int X_SIZE = 256;
    private static final int FULL_Y_SIZE = 204;
    private static final int CONTROLLER_Y_SIZE = 42;
    private static final int MAP_X = 137;
    private static final int POLLING_X = 173;
    private static final int SAVE_X = 209;
    private static final int INFO_X = 29;
    private static final int BUTTON_Y = 17;
    private static final int BUTTON_SIZE = 18;

    private final RadioTorchBlockEntity radio;
    private final Type type;
    private final Identifier texture;
    private final int ySize;
    private int guiLeft;
    private int guiTop;
    private @Nullable EditBox channel;
    private EditBox[] maps = new EditBox[0];
    private EditBox[] readerChannels = new EditBox[0];
    private EditBox[] readerNames = new EditBox[0];
    private int[] conditions = new int[0];

    public RadioTorchScreen(
            ScreenMenu<RadioTorchBlockEntity> menu, Inventory inventory, Component title) {
        super(menu, title);
        this.radio = menu.blockEntity();
        this.type = typeOf(radio);
        this.texture =
                RegistryUtil.id("textures/gui/machine/gui_rtty_" + type.textureName + ".png");
        this.ySize = type == Type.CONTROLLER ? CONTROLLER_Y_SIZE : FULL_Y_SIZE;
    }

    private static void click() {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    private static Type typeOf(RadioTorchBlockEntity radio) {
        if (radio instanceof RadioTorchSenderBlockEntity) return Type.SENDER;
        if (radio instanceof RadioTorchReceiverBlockEntity) return Type.RECEIVER;
        if (radio instanceof RadioLogicBlockEntity) return Type.LOGIC;
        if (radio instanceof RadioReaderBlockEntity) return Type.READER;
        if (radio instanceof RadioTorchControllerBlockEntity) return Type.CONTROLLER;
        throw new IllegalArgumentException("Counter uses ScreenRadioTorchCounter");
    }

    @Override
    protected void init() {
        guiLeft = (width - X_SIZE) / 2;
        guiTop = (height - ySize) / 2;

        switch (type) {
            case SENDER, RECEIVER -> {
                channel = field(guiLeft + 29, guiTop + 21, 82, radio.channel, 15);
                maps = new EditBox[16];
                int inset = type == Type.SENDER ? 18 : 0;
                for (int i = 0; i < maps.length; i++) {
                    maps[i] =
                            field(
                                    guiLeft + 11 + 130 * (i / 8) + inset,
                                    guiTop + 57 + 18 * (i % 8),
                                    82,
                                    radio.mapping[i],
                                    32);
                }
            }
            case LOGIC -> {
                RadioLogicBlockEntity logic = (RadioLogicBlockEntity) radio;
                channel = field(guiLeft + 29, guiTop + 21, 82, logic.channel, 15);
                maps = new EditBox[16];
                conditions = logic.conditionIndices.clone();
                for (int i = 0; i < maps.length; i++) {
                    maps[i] =
                            field(
                                    guiLeft + 29 + 130 * (i / 8),
                                    guiTop + 57 + 18 * (i % 8),
                                    46,
                                    logic.mapping[i],
                                    15);
                }
            }
            case READER -> {
                RadioReaderBlockEntity reader = (RadioReaderBlockEntity) radio;
                readerChannels = new EditBox[8];
                readerNames = new EditBox[8];
                for (int i = 0; i < 8; i++) {
                    readerChannels[i] =
                            field(guiLeft + 29, guiTop + 57 + i * 18, 64, reader.channels[i], 15);
                    readerNames[i] =
                            field(guiLeft + 123, guiTop + 57 + i * 18, 118, reader.names[i], 25);
                }
            }
            case CONTROLLER -> channel = field(guiLeft + 29, guiTop + 21, 82, radio.channel, 15);
        }
        updateFieldVisibility();
    }

    private EditBox field(int x, int y, int width, String value, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 14, Component.empty());
        field.setBordered(false);
        field.setTextColor(0xFF00FF00);
        field.setTextColorUneditable(0xFF00FF00);
        field.setMaxLength(maxLength);
        field.setValue(value);
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateFieldVisibility();
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        switch (type) {
            case SENDER, RECEIVER -> {
                if (radio.customMap) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            guiLeft,
                            guiTop,
                            0F,
                            0F,
                            X_SIZE,
                            FULL_Y_SIZE,
                            256,
                            256);
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            guiLeft + MAP_X,
                            guiTop + BUTTON_Y,
                            0F,
                            204F,
                            BUTTON_SIZE,
                            BUTTON_SIZE,
                            256,
                            256);
                } else {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            guiLeft,
                            guiTop,
                            0F,
                            0F,
                            X_SIZE,
                            35,
                            256,
                            256);
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            guiLeft,
                            guiTop + 35,
                            0F,
                            197F,
                            X_SIZE,
                            7,
                            256,
                            256);
                }
                if (radio.polling) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            guiLeft + POLLING_X,
                            guiTop + BUTTON_Y,
                            0F,
                            222F,
                            BUTTON_SIZE,
                            BUTTON_SIZE,
                            256,
                            256);
                }
            }
            case LOGIC -> {
                RadioLogicBlockEntity logic = (RadioLogicBlockEntity) radio;
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        guiLeft,
                        guiTop,
                        0F,
                        0F,
                        X_SIZE,
                        FULL_Y_SIZE,
                        256,
                        256);
                if (logic.descending) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            guiLeft + MAP_X,
                            guiTop + BUTTON_Y,
                            0F,
                            204F,
                            BUTTON_SIZE,
                            BUTTON_SIZE,
                            256,
                            256);
                }
                if (logic.polling) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            guiLeft + POLLING_X,
                            guiTop + BUTTON_Y,
                            0F,
                            222F,
                            BUTTON_SIZE,
                            BUTTON_SIZE,
                            256,
                            256);
                }
                for (int i = 0; i < maps.length; i++) {
                    int x = guiLeft + 7 + 130 * (i / 8);
                    int y = guiTop + 53 + 18 * (i % 8);
                    if (logic.mapping[i].isEmpty()) {
                        if (conditions[i] != 0) {
                            graphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    texture,
                                    x,
                                    y,
                                    18F + conditions[i] * 18F,
                                    222F,
                                    18,
                                    18,
                                    256,
                                    256);
                        }
                    } else {
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                texture,
                                x,
                                y,
                                18F + conditions[i] * 18F,
                                204F,
                                18,
                                18,
                                256,
                                256);
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                texture,
                                guiLeft + 85 + 130 * (i / 8),
                                guiTop + 57 + 18 * (i % 8),
                                198F,
                                204F,
                                14,
                                10,
                                256,
                                256);
                    }
                }
            }
            case READER -> {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        guiLeft,
                        guiTop,
                        0F,
                        0F,
                        X_SIZE,
                        FULL_Y_SIZE,
                        256,
                        256);
                if (radio.polling) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            guiLeft + POLLING_X,
                            guiTop + BUTTON_Y,
                            0F,
                            204F,
                            BUTTON_SIZE,
                            BUTTON_SIZE,
                            256,
                            256);
                }
            }
            case CONTROLLER -> {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        guiLeft,
                        guiTop,
                        0F,
                        0F,
                        X_SIZE,
                        CONTROLLER_Y_SIZE,
                        256,
                        256);
                if (radio.polling) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            guiLeft + POLLING_X,
                            guiTop + BUTTON_Y,
                            0F,
                            42F,
                            BUTTON_SIZE,
                            BUTTON_SIZE,
                            256,
                            256);
                }
            }
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(
                font,
                title,
                guiLeft + X_SIZE / 2 - font.width(title) / 2,
                guiTop + 6,
                0xFF404040,
                false);

        if (inButton(mouseX, mouseY, POLLING_X)) {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.literal(radio.polling ? "Polling" : "State Change"),
                    mouseX,
                    mouseY);
        } else if (inButton(mouseX, mouseY, SAVE_X)) {
            graphics.setTooltipForNextFrame(
                    font, Component.translatable("desc.shared.saveSettings"), mouseX, mouseY);
        } else if (inButton(mouseX, mouseY, MAP_X)) {
            if (type == Type.SENDER || type == Type.RECEIVER) {
                graphics.setTooltipForNextFrame(
                        font,
                        Component.literal(
                                radio.customMap ? "Custom Mapping" : "Redstone Passthrough"),
                        mouseX,
                        mouseY);
            } else if (type == Type.LOGIC) {
                graphics.setTooltipForNextFrame(
                        font,
                        Component.literal(
                                ((RadioLogicBlockEntity) radio).descending
                                        ? "Descending Order"
                                        : "Ascending Order"),
                        mouseX,
                        mouseY);
            }
        }

        int infoX = type == Type.CONTROLLER ? MAP_X : INFO_X;
        if ((type == Type.READER || type == Type.CONTROLLER) && inButton(mouseX, mouseY, infoX)) {
            showAttachedInfo(graphics, mouseX, mouseY);
        }

        if (type == Type.LOGIC) {
            for (int i = 0; i < conditions.length; i++) {
                if (inCondition(mouseX, mouseY, i)) {
                    graphics.setTooltipForNextFrame(
                            font,
                            Component.translatable("desc.gui.rttyLogic.cond" + conditions[i]),
                            mouseX,
                            mouseY);
                    break;
                }
            }
        }
    }

    private void showAttachedInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        RadioInfoProvider info = radio.attachedInfo();
        if (info == null) return;
        String prefix = type == Type.READER ? RadioCommandParser.VAL : RadioCommandParser.FUN;
        List<Component> lines = new ArrayList<>();
        lines.add(
                Component.literal(type == Type.READER ? "Readable values:" : "Usable functions:"));
        ChatFormatting color =
                type == Type.READER ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA;
        for (String entry : info.getFunctionInfo()) {
            if (entry.startsWith(prefix))
                lines.add(Component.literal(entry.substring(4)).withStyle(color));
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (inButton(mouseX, mouseY, MAP_X)) {
            if (type == Type.SENDER) {
                PacketWire.sendToServer(
                        RadioTorchSenderCustomMapLayout.encode(menu.containerId, !radio.customMap));
                click();
                return true;
            }
            if (type == Type.RECEIVER) {
                PacketWire.sendToServer(
                        RadioTorchReceiverCustomMapLayout.encode(
                                menu.containerId, !radio.customMap));
                click();
                return true;
            }
            if (type == Type.LOGIC) {
                PacketWire.sendToServer(
                        RadioTorchLogicDescendingLayout.encode(
                                menu.containerId, !((RadioLogicBlockEntity) radio).descending));
                click();
                return true;
            }
        }

        if (inButton(mouseX, mouseY, POLLING_X)) {
            sendPolling();
            click();
            return true;
        }

        if (inButton(mouseX, mouseY, SAVE_X)) {
            sendSettings();
            click();
            return true;
        }

        if (type == Type.LOGIC) {
            for (int i = 0; i < conditions.length; i++) {
                if (inCondition(mouseX, mouseY, i)) {
                    conditions[i] = (conditions[i] + 1) % 10;
                    click();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (type == Type.LOGIC && scrollY != 0D) {
            for (int i = 0; i < conditions.length; i++) {
                if (inCondition(mouseX, mouseY, i)) {
                    conditions[i] = (conditions[i] + (scrollY > 0D ? 1 : 9)) % 10;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void sendSettings() {
        switch (type) {
            case SENDER ->
                    PacketWire.sendToServer(
                            RadioTorchSenderSettingsLayout.encode(
                                    menu.containerId, channel.getValue(), this));
            case RECEIVER ->
                    PacketWire.sendToServer(
                            RadioTorchReceiverSettingsLayout.encode(
                                    menu.containerId, channel.getValue(), this));
            case LOGIC ->
                    PacketWire.sendToServer(
                            RadioTorchLogicSettingsLayout.encode(
                                    menu.containerId, channel.getValue(), this));
            case READER ->
                    PacketWire.sendToServer(
                            RadioTorchReaderSettingsLayout.encode(menu.containerId, this));
            case CONTROLLER ->
                    PacketWire.sendToServer(
                            RadioTorchControllerSettingsLayout.encode(
                                    menu.containerId, channel.getValue()));
        }
    }

    private void sendPolling() {
        switch (type) {
            case SENDER ->
                    PacketWire.sendToServer(
                            RadioTorchSenderPollingLayout.encode(menu.containerId, !radio.polling));
            case RECEIVER ->
                    PacketWire.sendToServer(
                            RadioTorchReceiverPollingLayout.encode(
                                    menu.containerId, !radio.polling));
            case LOGIC ->
                    PacketWire.sendToServer(
                            RadioTorchLogicPollingLayout.encode(menu.containerId, !radio.polling));
            case READER ->
                    PacketWire.sendToServer(
                            RadioTorchReaderPollingLayout.encode(menu.containerId, !radio.polling));
            case CONTROLLER ->
                    PacketWire.sendToServer(
                            RadioTorchControllerPollingLayout.encode(
                                    menu.containerId, !radio.polling));
        }
    }

    @Override
    public String entry(int index) {
        return maps[index].getValue();
    }

    @Override
    public int condition(int index) {
        return conditions[index];
    }

    @Override
    public String channel(int index) {
        return readerChannels[index].getValue();
    }

    @Override
    public String name(int index) {
        return readerNames[index].getValue();
    }

    private void updateFieldVisibility() {
        if (type == Type.SENDER || type == Type.RECEIVER) {
            for (EditBox map : maps) map.visible = radio.customMap;
        }
    }

    private boolean inButton(double mouseX, double mouseY, int x) {
        return guiLeft + x <= mouseX
                && guiLeft + x + BUTTON_SIZE > mouseX
                && guiTop + BUTTON_Y < mouseY
                && guiTop + BUTTON_Y + BUTTON_SIZE >= mouseY;
    }

    private boolean inCondition(double mouseX, double mouseY, int index) {
        int x = guiLeft + 7 + 130 * (index / 8);
        int y = guiTop + 53 + 18 * (index % 8);
        return x <= mouseX && x + 18 > mouseX && y <= mouseY && y + 18 > mouseY;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft != null
                && minecraft.options.keyInventory.matches(event)
                && !(getFocused() instanceof EditBox box && box.canConsumeInput())) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Type {
        SENDER("sender"),
        RECEIVER("receiver"),
        LOGIC("logic_receiver"),
        READER("reader"),
        CONTROLLER("controller");

        private final String textureName;

        Type(String textureName) {
            this.textureName = textureName;
        }
    }
}

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

import com.hbm.blockentity.network.RadioTelexBlockEntity;
import com.hbm.client.gui.GuiLineStrip;
import com.hbm.client.gui.MenuScreen;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RadioTelexChannelsLayout;
import com.hbm.packet.layout.RadioTelexClearLayout;
import com.hbm.packet.layout.RadioTelexPrintLayout;
import com.hbm.packet.layout.RadioTelexSendLayout;
import com.hbm.packet.layout.RadioTelexTransmitBufferLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class RadioTelexScreen extends MenuScreen<ScreenMenu<RadioTelexBlockEntity>> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/machine/gui_telex.png");
    private static final int X_SIZE = 256;
    private static final int Y_SIZE = 244;
    private static final int BUFFER_LINES = 5;
    private static final int GREEN = 0xFF00FF00;

    private final RadioTelexBlockEntity telex;
    private final String[] txBuffer = new String[BUFFER_LINES];
    private int guiLeft;
    private int guiTop;
    private EditBox txFrequency;
    private EditBox rxFrequency;
    private boolean textFocus;
    private int cursorPos;

    public RadioTelexScreen(
            ScreenMenu<RadioTelexBlockEntity> menu, Inventory inventory, Component title) {
        super(menu, title);
        this.telex = menu.blockEntity();
        System.arraycopy(telex.txBuffer, 0, txBuffer, 0, BUFFER_LINES);
        for (int i = BUFFER_LINES - 1; i > 0; i--) {
            if (!txBuffer[i].isEmpty()) {
                cursorPos = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        guiLeft = (width - X_SIZE) / 2;
        guiTop = (height - Y_SIZE) / 2;

        txFrequency = new EditBox(font, guiLeft + 29, guiTop + 110, 90, 14, Component.empty());
        txFrequency.setTextColor(GREEN);
        txFrequency.setTextColorUneditable(GREEN);
        txFrequency.setBordered(false);
        txFrequency.setMaxLength(10);
        txFrequency.setValue(telex.txChannel == null ? "" : telex.txChannel);
        addRenderableWidget(txFrequency);

        rxFrequency = new EditBox(font, guiLeft + 29, guiTop + 224, 90, 14, Component.empty());
        rxFrequency.setTextColor(GREEN);
        rxFrequency.setTextColorUneditable(GREEN);
        rxFrequency.setBordered(false);
        rxFrequency.setMaxLength(10);
        rxFrequency.setValue(telex.rxChannel == null ? "" : telex.rxChannel);
        addRenderableWidget(rxFrequency);
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
        drawTransmitBuffer(graphics);
        drawReceiveBuffer(graphics);
        drawWaveform(graphics);
        drawTooltips(graphics, mouseX, mouseY);
    }

    private void drawTransmitBuffer(GuiGraphicsExtractor graphics) {
        boolean blink = minecraft.gui.hud.getGuiTicks() % 20 < 10;
        for (int line = 0; line < BUFFER_LINES; line++) {
            String text = txBuffer[line];
            int y = 11 + 14 * line;
            String format = ChatFormatting.RESET.toString();

            for (int index = 0; index < text.length(); index++) {
                int x = 11 + 7 * index;
                char c = text.charAt(index);
                x += (7 - font.width(String.valueOf(c))) / 2;
                if (c == '§' && text.length() > index + 1) {
                    format = "§" + text.charAt(index + 1);
                    x -= 3;
                }
                String glyph = format + c;
                if (c == RadioTelexBlockEntity.BELL) glyph = ChatFormatting.RED + "B";
                if (c == RadioTelexBlockEntity.PRINT) glyph = ChatFormatting.RED + "P";
                if (c == RadioTelexBlockEntity.CLEAR) glyph = ChatFormatting.RED + "<";
                if (c == RadioTelexBlockEntity.PAUSE) glyph = ChatFormatting.RED + "W";
                graphics.text(font, glyph, guiLeft + x, guiTop + y, GREEN, false);
            }

            if (blink && textFocus && cursorPos == line) {
                int x = Math.max(11 + 7 * (text.length() - 1) + 7, 11);
                graphics.text(font, "|", guiLeft + x, guiTop + y, GREEN, false);
            }
        }
    }

    private void drawReceiveBuffer(GuiGraphicsExtractor graphics) {
        for (int line = 0; line < BUFFER_LINES; line++) {
            String text = telex.rxBuffer[line];
            int y = 145 + 14 * line;
            String format = ChatFormatting.RESET.toString();
            int x = 11;

            for (int index = 0; index < text.length(); index++) {
                char c = text.charAt(index);
                x += (7 - font.width(String.valueOf(c))) / 2;
                if (c == '§' && text.length() > index + 1) {
                    format = "§" + text.charAt(index + 1);
                    c = ' ';
                } else if (c == '§') {
                    c = ' ';
                } else if (index > 0 && text.charAt(index - 1) == '§') {
                    c = ' ';
                    x -= 14;
                }
                graphics.text(font, format + c, guiLeft + x, guiTop + y, GREEN, false);
                x += 7;
            }
        }
    }

    private void drawWaveform(GuiGraphicsExtractor graphics) {
        float[] points = new float[49 * 2];
        Random random = new Random(telex.sendingChar);
        double offset = 0;
        for (int i = 0; i < 48; i++) {
            points[i * 2] = guiLeft + 199 + i;
            points[i * 2 + 1] = (float) (guiTop + 93.5D + offset);
            offset = telex.sendingChar != ' ' && i > 4 && i < 43 ? random.nextGaussian() * 7D : 0D;
            offset = Math.clamp(offset, -7D, 7D);
            points[(i + 1) * 2] = guiLeft + 199 + i + 1;
            points[(i + 1) * 2 + 1] = (float) (guiTop + 93.5D + offset);
        }
        GuiLineStrip.draw(graphics, points, GREEN);
    }

    private void drawTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        tooltip(
                graphics,
                mouseX,
                mouseY,
                7,
                85,
                Component.translatable("desc.gui.radioTelex.bell").withStyle(ChatFormatting.GOLD),
                Component.translatable("desc.gui.radioTelex.playsABellWhen"));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                27,
                85,
                Component.translatable("desc.gui.radioTelex.print").withStyle(ChatFormatting.GOLD),
                Component.translatable("desc.gui.radioTelex.forcesRecipientToPrint"));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                47,
                85,
                Component.translatable("desc.gui.radioTelex.clearScreen")
                        .withStyle(ChatFormatting.GOLD),
                Component.translatable("desc.gui.radioTelex.wipesMessageBufferWhen"));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                67,
                85,
                Component.translatable("desc.gui.radioTelex.format").withStyle(ChatFormatting.GOLD),
                Component.translatable("desc.gui.radioTelex.insertsFormatCharacterFor"));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                87,
                85,
                Component.translatable("desc.gui.radioTelex.pause").withStyle(ChatFormatting.GOLD),
                Component.translatable("desc.gui.radioTelex.pausesMessageTransmission"));

        tooltip(
                graphics,
                mouseX,
                mouseY,
                127,
                105,
                Component.translatable("desc.gui.radioTelex.saveId")
                        .withStyle(ChatFormatting.GREEN));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                147,
                105,
                Component.translatable("desc.gui.radioTelex.sendMessage")
                        .withStyle(ChatFormatting.YELLOW));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                167,
                105,
                Component.translatable("desc.gui.radioTelex.deleteMessageBuffer")
                        .withStyle(ChatFormatting.RED));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                127,
                219,
                Component.translatable("desc.gui.radioTelex.saveId")
                        .withStyle(ChatFormatting.GREEN));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                147,
                219,
                Component.translatable("desc.gui.radioTelex.printMessage")
                        .withStyle(ChatFormatting.AQUA));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                167,
                219,
                Component.translatable("desc.gui.radioTelex.clearScreen")
                        .withStyle(ChatFormatting.RED));
    }

    private void tooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            Component... lines) {
        if (inZone(mouseX, mouseY, x, y)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(lines), mouseX, mouseY);
        }
    }

    private boolean inZone(double mouseX, double mouseY, int x, int y) {
        return guiLeft + x <= mouseX
                && guiLeft + x + 18 > mouseX
                && guiTop + y < mouseY
                && guiTop + y + 18 >= mouseY;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        int x = (int) event.x();
        int y = (int) event.y();
        textFocus = guiLeft + 7 <= x && guiLeft + 249 > x && guiTop + 7 < y && guiTop + 81 >= y;
        if (textFocus) {
            setTextFocus();
            handled = true;
        }

        char character = '\0';
        String command = null;
        if (inZone(x, y, 7, 85)) character = RadioTelexBlockEntity.BELL;
        if (inZone(x, y, 27, 85)) character = RadioTelexBlockEntity.PRINT;
        if (inZone(x, y, 47, 85)) character = RadioTelexBlockEntity.CLEAR;
        if (inZone(x, y, 67, 85)) character = '§';
        if (inZone(x, y, 87, 85)) character = RadioTelexBlockEntity.PAUSE;

        if (inZone(x, y, 127, 105) || inZone(x, y, 127, 219)) command = "sve";
        if (inZone(x, y, 147, 105)) command = "snd";
        if (inZone(x, y, 167, 105)) {
            command = "rxdel";
            for (int i = 0; i < BUFFER_LINES; i++) txBuffer[i] = "";
            sendTxBuffer();
        }
        if (inZone(x, y, 147, 219)) command = "rxprt";
        if (inZone(x, y, 167, 219)) command = "rxcls";

        if (command != null) {
            playButtonSound();
            switch (command) {
                case "sve" ->
                        PacketWire.sendToServer(
                                RadioTelexChannelsLayout.encode(
                                        menu.containerId,
                                        txFrequency.getValue(),
                                        rxFrequency.getValue()));
                case "snd" ->
                        PacketWire.sendToServer(RadioTelexSendLayout.encode(menu.containerId));
                case "rxprt" ->
                        PacketWire.sendToServer(RadioTelexPrintLayout.encode(menu.containerId));
                case "rxcls" ->
                        PacketWire.sendToServer(RadioTelexClearLayout.encode(menu.containerId));
                case "rxdel" -> {}
                default -> throw new IllegalArgumentException(command);
            }
            handled = true;
        }

        if (character != '\0') {
            playButtonSound();
            setTextFocus();
            submitChar(character);
            handled = true;
        }

        return handled;
    }

    private void setTextFocus() {
        textFocus = true;
        txFrequency.setFocused(false);
        rxFrequency.setFocused(false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!event.isEscape() && (txFrequency.canConsumeInput() || rxFrequency.canConsumeInput())) {
            super.keyPressed(event);
            return true;
        }
        if (textFocus) {
            if (event.isEscape()) {
                textFocus = false;
                return true;
            }
            if (event.key() == InputConstants.KEY_UP) cursorPos--;
            if (event.key() == InputConstants.KEY_DOWN) cursorPos++;
            cursorPos = Math.clamp(cursorPos, 0, BUFFER_LINES - 1);
            if (event.key() == InputConstants.KEY_BACKSPACE && !txBuffer[cursorPos].isEmpty()) {
                String line = txBuffer[cursorPos];
                txBuffer[cursorPos] = line.substring(0, line.length() - 1);
            }
            return true;
        }

        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (super.charTyped(event)) return true;
        if (textFocus && event.isAllowedChatCharacter()) {
            submitChar((char) event.codepoint());
            return true;
        }
        return false;
    }

    private void submitChar(char c) {
        String line = txBuffer[cursorPos];
        if (line.length() < RadioTelexBlockEntity.LINE_WIDTH) txBuffer[cursorPos] = line + c;
    }

    @Override
    public void onClose() {
        sendTxBuffer();
        super.onClose();
    }

    private void sendTxBuffer() {
        PacketWire.sendToServer(RadioTelexTransmitBufferLayout.encode(menu.containerId, txBuffer));
    }

    private void playButtonSound() {
        if (minecraft != null)
            minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

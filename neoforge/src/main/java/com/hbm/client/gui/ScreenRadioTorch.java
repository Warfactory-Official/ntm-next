// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.BlockEntityRadioTorch;
import com.hbm.tileentity.network.BlockEntityRadioTorchController;
import com.hbm.tileentity.network.BlockEntityRadioTorchLogic;
import com.hbm.tileentity.network.BlockEntityRadioTorchReader;
import com.hbm.tileentity.network.BlockEntityRadioTorchReceiver;
import com.hbm.tileentity.network.BlockEntityRadioTorchSender;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

public final class ScreenRadioTorch extends Screen {

    private static final int X_SIZE = 256;
    private static final int FULL_Y_SIZE = 204;
    private static final int CONTROLLER_Y_SIZE = 42;
    private static final int MAP_X = 137;
    private static final int POLLING_X = 173;
    private static final int SAVE_X = 209;
    private static final int INFO_X = 29;
    private static final int BUTTON_Y = 17;
    private static final int BUTTON_SIZE = 18;

    private final BlockEntityRadioTorch radio;
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

    public ScreenRadioTorch(BlockEntityRadioTorch radio) {
        super(Component.translatable(titleKey(typeOf(radio))));
        this.radio = radio;
        this.type = typeOf(radio);
        this.texture = Library.id("textures/gui/machine/gui_rtty_" + type.textureName + ".png");
        this.ySize = type == Type.CONTROLLER ? CONTROLLER_Y_SIZE : FULL_Y_SIZE;
    }

    private static void click() {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    private static Type typeOf(BlockEntityRadioTorch radio) {
        if (radio instanceof BlockEntityRadioTorchSender) return Type.SENDER;
        if (radio instanceof BlockEntityRadioTorchReceiver) return Type.RECEIVER;
        if (radio instanceof BlockEntityRadioTorchLogic) return Type.LOGIC;
        if (radio instanceof BlockEntityRadioTorchReader) return Type.READER;
        if (radio instanceof BlockEntityRadioTorchController) return Type.CONTROLLER;
        throw new IllegalArgumentException("Counter uses ScreenRadioTorchCounter");
    }

    private static String titleKey(Type type) {
        return switch (type) {
            case SENDER -> "container.rttySender";
            case RECEIVER -> "container.rttyReceiver";
            case LOGIC -> "container.rttyLogic";
            case READER -> "container.rttyReader";
            case CONTROLLER -> "container.rttyController";
        };
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
                BlockEntityRadioTorchLogic logic = (BlockEntityRadioTorchLogic) radio;
                channel = field(guiLeft + 29, guiTop + 21, 82, logic.channel, 15);
                maps = new EditBox[16];
                conditions = logic.conditions.clone();
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
                BlockEntityRadioTorchReader reader = (BlockEntityRadioTorchReader) radio;
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
                BlockEntityRadioTorchLogic logic = (BlockEntityRadioTorchLogic) radio;
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
        Component title = Component.translatable(titleKey(type));
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
                                ((BlockEntityRadioTorchLogic) radio).descending
                                        ? "Descending Order"
                                        : "Ascending Order"),
                        mouseX,
                        mouseY);
            }
        }

        if ((type == Type.READER || type == Type.CONTROLLER) && inButton(mouseX, mouseY, INFO_X)) {
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
        IRORInfo info = radio.attachedInfo();
        if (info == null) return;
        String prefix = type == Type.READER ? IRORInfo.PREFIX_VALUE : IRORInfo.PREFIX_FUNCTION;
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
            if (type == Type.SENDER || type == Type.RECEIVER) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("m", !radio.customMap);
                send(data);
                click();
                return true;
            }
            if (type == Type.LOGIC) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("d", !((BlockEntityRadioTorchLogic) radio).descending);
                send(data);
                click();
                return true;
            }
        }

        if (inButton(mouseX, mouseY, POLLING_X)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("p", !radio.polling);
            send(data);
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
        CompoundTag data = new CompoundTag();
        switch (type) {
            case SENDER, RECEIVER -> {
                data.putString("c", channel.getValue());
                for (int i = 0; i < maps.length; i++) data.putString("m" + i, maps[i].getValue());
            }
            case LOGIC -> {
                data.putString("c", channel.getValue());
                for (int i = 0; i < maps.length; i++) {
                    data.putString("m" + i, maps[i].getValue());
                    data.putInt("c" + i, conditions[i]);
                }
            }
            case READER -> {
                for (int i = 0; i < readerChannels.length; i++) {
                    data.putString("c" + i, readerChannels[i].getValue());
                    data.putString("n" + i, readerNames[i].getValue());
                }
            }
            case CONTROLLER -> data.putString("c", channel.getValue());
        }
        send(data);
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

    private void send(CompoundTag data) {
        Services.NETWORK.sendToServer(new NbtControlPayload(radio.getBlockPos(), data));
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

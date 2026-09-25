// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge;
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

public final class ScreenRBMKGauge extends Screen {

    private static final int X_SIZE = 256;
    private static final int Y_SIZE = 204;
    private static final int GAUGES = BlockEntityRBMKGauge.GAUGES;
    private static final int ROW_PITCH = 36;
    private static final int ACTIVE_X = 111;
    private static final int ACTIVE_SIZE = 16;
    private static final int POLLING_X = 128;
    private static final int TOGGLE_SIZE = 18;
    private static final int SAVE_X = 209;
    private static final int SAVE_Y = 17;
    private static final int MAX_CHAN_LENGTH = 15;

    private final BlockEntityRBMKGauge gauge;
    private final Identifier texture = Library.id("textures/gui/machine/gui_rbmk_gauge.png");

    private final EditBox[] color = new EditBox[GAUGES];
    private final EditBox[] label = new EditBox[GAUGES];
    private final EditBox[] rtty = new EditBox[GAUGES];
    private final EditBox[] min = new EditBox[GAUGES];
    private final EditBox[] max = new EditBox[GAUGES];
    private final boolean[] active = new boolean[GAUGES];
    private final boolean[] polling = new boolean[GAUGES];

    private int guiLeft;
    private int guiTop;

    public ScreenRBMKGauge(BlockEntityRBMKGauge gauge) {
        super(Component.translatable("container.rbmkGauge"));
        this.gauge = gauge;
    }

    private static void click(float pitch) {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    @Override
    protected void init() {
        guiLeft = (width - X_SIZE) / 2;
        guiTop = (height - Y_SIZE) / 2;

        int oX = 4;
        int oY = 4;

        for (int i = 0; i < GAUGES; i++) {
            BlockEntityRBMKGauge.GaugeUnit unit = gauge.gauges[i];
            String col = Integer.toHexString(unit.color);
            while (col.length() < 6) col = "0" + col;

            color[i] =
                    field(guiLeft + 27 + oX, guiTop + 55 + oY + i * ROW_PITCH, 72 - oX * 2, col, 6);
            label[i] =
                    field(
                            guiLeft + 175 + oX,
                            guiTop + 55 + oY + i * ROW_PITCH,
                            72 - oX * 2,
                            unit.label,
                            15);
            rtty[i] =
                    field(
                            guiLeft + 27 + oX,
                            guiTop + 73 + oY + i * ROW_PITCH,
                            72 - oX * 2,
                            unit.rtty,
                            MAX_CHAN_LENGTH);
            min[i] =
                    field(
                            guiLeft + 121 + oX,
                            guiTop + 73 + oY + i * ROW_PITCH,
                            52 - oX * 2,
                            unit.min + "",
                            32);
            max[i] =
                    field(
                            guiLeft + 195 + oX,
                            guiTop + 73 + oY + i * ROW_PITCH,
                            52 - oX * 2,
                            unit.max + "",
                            32);

            active[i] = unit.active;
            polling[i] = unit.polling;
        }
    }

    private EditBox field(int x, int y, int width, String value, int maxLength) {
        EditBox box = new EditBox(font, x, y, width, 14, Component.empty());
        box.setBordered(false);
        box.setTextColor(0xFF00FF00);
        box.setTextColorUneditable(0xFF00FF00);
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                guiLeft,
                guiTop,
                0F,
                0F,
                X_SIZE,
                Y_SIZE,
                256,
                256);

        for (int i = 0; i < GAUGES; i++) {
            if (active[i]) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        guiLeft + ACTIVE_X,
                        guiTop + i * ROW_PITCH + 54,
                        18F,
                        204F,
                        ACTIVE_SIZE,
                        ACTIVE_SIZE,
                        256,
                        256);
            }
            if (polling[i]) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        guiLeft + POLLING_X,
                        guiTop + i * ROW_PITCH + 53,
                        0F,
                        204F,
                        TOGGLE_SIZE,
                        TOGGLE_SIZE,
                        256,
                        256);
            }
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Component name = Component.translatable("container.rbmkGauge");
        graphics.text(
                font,
                name,
                guiLeft + X_SIZE / 2 - font.width(name) / 2,
                guiTop + 6,
                0xFF404040,
                false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x();
        double y = event.y();

        for (int i = 0; i < GAUGES; i++) {
            if (guiLeft + ACTIVE_X <= x
                    && guiLeft + ACTIVE_X + ACTIVE_SIZE > x
                    && guiTop + i * ROW_PITCH + 54 < y
                    && guiTop + i * ROW_PITCH + 54 + ACTIVE_SIZE >= y) {
                active[i] = !active[i];
                click(0.5F + (active[i] ? 0.25F : 0F));
                return true;
            }

            if (guiLeft + POLLING_X <= x
                    && guiLeft + POLLING_X + TOGGLE_SIZE > x
                    && guiTop + i * ROW_PITCH + 53 < y
                    && guiTop + i * ROW_PITCH + 53 + TOGGLE_SIZE >= y) {
                polling[i] = !polling[i];
                click(0.5F + (polling[i] ? 0.25F : 0F));
                return true;
            }
        }

        if (guiLeft + SAVE_X <= x
                && guiLeft + SAVE_X + TOGGLE_SIZE > x
                && guiTop + SAVE_Y < y
                && guiTop + SAVE_Y + TOGGLE_SIZE >= y) {
            click(1.0F);
            sendSettings();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void sendSettings() {
        CompoundTag data = new CompoundTag();
        byte activeMask = 0;
        byte pollingMask = 0;
        for (int i = 0; i < GAUGES; i++) {
            if (active[i]) activeMask |= (byte) (1 << i);
            if (polling[i]) pollingMask |= (byte) (1 << i);
        }
        data.putByte("active", activeMask);
        data.putByte("polling", pollingMask);

        for (int i = 0; i < GAUGES; i++) {

            try {
                data.putInt("color" + i, Integer.parseInt(color[i].getValue(), 16));
            } catch (Exception ex) {
            }
            data.putString("label" + i, label[i].getValue());
            data.putString("rtty" + i, rtty[i].getValue());
            try {
                data.putInt("min" + i, Integer.parseInt(min[i].getValue()));
            } catch (Exception ex) {
            }
            try {
                data.putInt("max" + i, Integer.parseInt(max[i].getValue()));
            } catch (Exception ex) {
            }
        }
        Services.NETWORK.sendToServer(new NbtControlPayload(gauge.getBlockPos(), data));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (getFocused() instanceof EditBox) return super.keyPressed(event);
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

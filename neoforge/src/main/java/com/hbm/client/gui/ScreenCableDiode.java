// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.BlockEntityCableDiode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class ScreenCableDiode extends Screen {

    private final BlockEntityCableDiode diode;
    private EditBox throughput;
    private int priority;

    public ScreenCableDiode(BlockEntityCableDiode diode) {
        super(Component.empty());
        this.diode = diode;
        this.priority = diode.priority.ordinal();
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
        throughput.setValue(Long.toString(diode.limit));
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
        CompoundTag data = new CompoundTag();
        data.putByte("priority", (byte) priority);
        try {
            data.putLong("limit", Long.parseLong(throughput.getValue()));
        } catch (NumberFormatException ignored) {
        }
        Services.NETWORK.sendToServer(new NbtControlPayload(diode.getBlockPos(), data));
        super.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
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

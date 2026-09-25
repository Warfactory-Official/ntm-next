// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.BlockEntityFluidPump;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class ScreenFluidPump extends Screen {

    private final BlockEntityFluidPump pump;
    private EditBox throughput;
    private int pressure;
    private int priority;

    public ScreenFluidPump(BlockEntityFluidPump pump) {
        super(Component.translatable("block.hbm.fluid_pump"));
        this.pump = pump;
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
        CompoundTag data = new CompoundTag();
        data.putByte("pressure", (byte) pressure);
        data.putByte("priority", (byte) priority);
        try {
            data.putInt("rate", Integer.parseInt(throughput.getValue().trim()));
        } catch (NumberFormatException ignored) {
        }
        Services.NETWORK.sendToServer(new NbtControlPayload(pump.getBlockPos(), data));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

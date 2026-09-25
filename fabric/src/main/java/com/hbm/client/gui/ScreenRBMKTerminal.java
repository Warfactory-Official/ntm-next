// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ScreenRBMKTerminal extends Screen {

    private static final int MAX_LINE = 50;
    private static final String[] LEGEND = {
        "[Esc] - Quit",
        "chan <channel> - Set selected channel",
        "send <cmd> - Send single signal over selected channel",
        "start <cmd> - Continuously send signal over selected channel",
        "stop - Stop continuous sending",
        "clear - Delete command history",
    };

    private final BlockEntityRBMKTerminal terminal;
    private EditBox line;

    public ScreenRBMKTerminal(BlockEntityRBMKTerminal terminal) {
        super(Component.empty());
        this.terminal = terminal;
    }

    public static boolean isEditing(BlockEntityRBMKTerminal terminal) {
        return Minecraft.getInstance().gui.screen() instanceof ScreenRBMKTerminal gui
                && gui.terminal == terminal;
    }

    public static String getWorkingLine(BlockEntityRBMKTerminal terminal) {
        return isEditing(terminal)
                ? ((ScreenRBMKTerminal) Minecraft.getInstance().gui.screen()).line.getValue()
                : "";
    }

    @Override
    protected void init() {
        line = new EditBox(font, 0, 0, 0, 0, Component.empty());
        line.setMaxLength(MAX_LINE);
        setFocused(line);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        for (int i = 0; i < LEGEND.length; i++) {
            graphics.text(font, Component.literal(LEGEND[i]), 2, 2 + i * 10, 0xFFFFFFFF, false);
        }
        graphics.pose().popMatrix();
    }

    @Override
    public void tick() {

        if (terminal.isRemoved()) onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            CompoundTag data = new CompoundTag();
            data.putString("cmd", line.getValue());
            Services.NETWORK.sendToServer(new NbtControlPayload(terminal.getBlockPos(), data));
            line.setValue("");
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_HOME
                || event.key() == GLFW.GLFW_KEY_LEFT
                || event.key() == GLFW.GLFW_KEY_RIGHT
                || event.key() == GLFW.GLFW_KEY_END) {
            return true;
        }

        line.moveCursorToEnd(false);
        return line.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return line.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

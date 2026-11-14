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

import com.hbm.blockentity.machine.rbmk.RBMKTerminalBlockEntity;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RBMKTerminalCommandLayout;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class RBMKTerminalScreen extends MenuScreen<ScreenMenu<RBMKTerminalBlockEntity>> {

    private static final int MAX_LINE = 50;
    private static final String[] LEGEND = {
        "[Esc] - Quit",
        "chan <channel> - Set selected channel",
        "send <cmd> - Send single signal over selected channel",
        "start <cmd> - Continuously send signal over selected channel",
        "stop - Stop continuous sending",
        "clear - Delete command history",
    };

    private final RBMKTerminalBlockEntity terminal;
    private EditBox line;

    public RBMKTerminalScreen(
            ScreenMenu<RBMKTerminalBlockEntity> menu, Inventory inventory, Component title) {
        super(menu, title);
        this.terminal = menu.blockEntity();
    }

    public static boolean isEditing(RBMKTerminalBlockEntity terminal) {
        return Minecraft.getInstance().gui.screen() instanceof RBMKTerminalScreen gui
                && gui.terminal == terminal;
    }

    public static String getWorkingLine(RBMKTerminalBlockEntity terminal) {
        return isEditing(terminal)
                ? ((RBMKTerminalScreen) Minecraft.getInstance().gui.screen()).line.getValue()
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
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (event.key() == InputConstants.KEY_RETURN
                || event.key() == InputConstants.KEY_NUMPADENTER) {
            PacketWire.sendToServer(
                    RBMKTerminalCommandLayout.encode(menu.containerId, line.getValue()));
            line.setValue("");
            return true;
        }

        if (event.key() == InputConstants.KEY_HOME
                || event.key() == InputConstants.KEY_LEFT
                || event.key() == InputConstants.KEY_RIGHT
                || event.key() == InputConstants.KEY_END) {
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

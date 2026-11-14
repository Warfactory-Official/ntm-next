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

import com.hbm.blockentity.machine.rbmk.RBMKGraphBlockEntity;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RBMKGraphSettingsLayout;
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

public final class RBMKGraphScreen extends MenuScreen<ScreenMenu<RBMKGraphBlockEntity>>
        implements RBMKGraphSettingsLayout.Graphs {

    private static final int X_SIZE = 256;
    private static final int Y_SIZE = 150;
    private static final int GRAPHS = RBMKGraphBlockEntity.GRAPHS;
    private static final int ROW_PITCH = 54;
    private static final int ACTIVE_X = 111;
    private static final int ACTIVE_SIZE = 16;
    private static final int POLLING_X = 128;
    private static final int TOGGLE_SIZE = 18;
    private static final int SAVE_X = 209;
    private static final int SAVE_Y = 17;
    private static final int MAX_CHAN_LENGTH = 15;

    private final RBMKGraphBlockEntity graph;
    private final Identifier texture = RegistryUtil.id("textures/gui/machine/gui_rbmk_graph.png");

    private final EditBox[] label = new EditBox[GRAPHS];
    private final EditBox[] rtty = new EditBox[GRAPHS];
    private final EditBox[] min = new EditBox[GRAPHS];
    private final EditBox[] max = new EditBox[GRAPHS];
    private final boolean[] active = new boolean[GRAPHS];
    private final boolean[] polling = new boolean[GRAPHS];

    private int guiLeft;
    private int guiTop;

    public RBMKGraphScreen(
            ScreenMenu<RBMKGraphBlockEntity> menu, Inventory inventory, Component title) {
        super(menu, title);
        this.graph = menu.blockEntity();
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

        for (int i = 0; i < GRAPHS; i++) {
            RBMKGraphBlockEntity.GraphUnit unit = graph.graphs[i];
            label[i] =
                    field(
                            guiLeft + 27 + oX,
                            guiTop + 73 + oY + i * ROW_PITCH,
                            72 - oX * 2,
                            unit.label,
                            30);
            rtty[i] =
                    field(
                            guiLeft + 27 + oX,
                            guiTop + 55 + oY + i * ROW_PITCH,
                            72 - oX * 2,
                            unit.rtty,
                            MAX_CHAN_LENGTH);
            min[i] =
                    field(
                            guiLeft + 175 + oX,
                            guiTop + 55 + oY + i * ROW_PITCH,
                            72 - oX * 2,
                            unit.minBound ? unit.min + "" : "",
                            15);
            max[i] =
                    field(
                            guiLeft + 175 + oX,
                            guiTop + 73 + oY + i * ROW_PITCH,
                            72 - oX * 2,
                            unit.maxBound ? unit.max + "" : "",
                            15);

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

        for (int i = 0; i < GRAPHS; i++) {
            if (active[i]) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        guiLeft + ACTIVE_X,
                        guiTop + i * ROW_PITCH + 54,
                        18F,
                        150F,
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
                        150F,
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
        Component name = Component.translatable("container.rbmkGraph");
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

        for (int i = 0; i < GRAPHS; i++) {
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
        PacketWire.sendToServer(RBMKGraphSettingsLayout.encode(menu.containerId, this));
    }

    @Override
    public boolean active(int index) {
        return active[index];
    }

    @Override
    public boolean polling(int index) {
        return polling[index];
    }

    @Override
    public String label(int index) {
        return label[index].getValue();
    }

    @Override
    public String channel(int index) {
        return rtty[index].getValue();
    }

    @Override
    public boolean minimumPresent(int index) {
        return bound(min[index]);
    }

    @Override
    public long minimum(int index) {
        return bound(min[index]) ? Long.parseLong(min[index].getValue()) : 0L;
    }

    @Override
    public boolean maximumPresent(int index) {
        return bound(max[index]);
    }

    @Override
    public long maximum(int index) {
        return bound(max[index]) ? Long.parseLong(max[index].getValue()) : 0L;
    }

    private static boolean bound(EditBox field) {
        try {
            Long.parseLong(field.getValue());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (getFocused() instanceof EditBox) return super.keyPressed(event);
        if (minecraft.options.keyInventory.matches(event)) {
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

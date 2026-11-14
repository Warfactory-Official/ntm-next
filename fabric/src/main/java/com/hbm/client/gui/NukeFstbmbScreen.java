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

import com.hbm.blockentity.bomb.BalefireNukeBlockEntity;
import com.hbm.inventory.container.NukeFstbmbMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.BalefireStartLayout;
import com.hbm.packet.layout.BalefireTimerLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class NukeFstbmbScreen extends InfoContainerScreen<NukeFstbmbMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/fstbmb_schematic.png");

    private static final int FIELD_X = 94, FIELD_Y = 40, FIELD_W = 29, FIELD_H = 12;
    private static final int START_X = 142, START_Y = 35, START_SIZE = 18;
    private static final int EGG_X = 19, EGG_Y = 90, EGG_W = 30, EGG_H = 16;
    private static final int BATTERY_X = 88, BATTERY_Y = 93, BATTERY_W = 18, BATTERY_H = 10;
    private static final float CLOCK_SCALE = 0.75F;
    private static final int CLOCK_CENTRE = 69, CLOCK_Y = 95;

    private @Nullable EditBox field;

    public NukeFstbmbScreen(NukeFstbmbMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
    }

    @Override
    protected void init() {
        super.init();
        field =
                new EditBox(
                        font,
                        leftPos + FIELD_X,
                        topPos + FIELD_Y,
                        FIELD_W,
                        FIELD_H,
                        Component.empty());
        field.setTextColor(0xFFFF0000);
        field.setTextColorUneditable(0xFF800000);
        field.setBordered(false);
        field.setMaxLength(3);
        field.setValue(String.valueOf(menu.blockEntity().timer / 20));
        field.setResponder(this::commit);
        addRenderableWidget(field);
    }

    private void commit(String text) {
        if (text.isEmpty() || !text.chars().allMatch(Character::isDigit)) return;
        int seconds =
                Math.clamp(
                        Integer.parseInt(text),
                        BalefireNukeBlockEntity.MIN_SECONDS,
                        BalefireNukeBlockEntity.MAX_SECONDS);
        PacketWire.sendToServer(BalefireTimerLayout.encode(menu.containerId, seconds));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && !menu.blockEntity().started
                && checkClick(
                        (int) event.x(),
                        (int) event.y(),
                        START_X,
                        START_Y,
                        START_SIZE,
                        START_SIZE)) {
            PacketWire.sendToServer(BalefireStartLayout.encode(menu.containerId));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        BalefireNukeBlockEntity bomb = menu.blockEntity();

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        if (bomb.hasEgg()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + EGG_X,
                    topPos + EGG_Y,
                    176.0F,
                    0.0F,
                    EGG_W,
                    EGG_H,
                    256,
                    256);
        }
        int battery = bomb.getBattery();
        if (battery != BalefireNukeBlockEntity.BATTERY_NONE) {
            float u = battery == BalefireNukeBlockEntity.BATTERY_SPARK ? 176.0F : 194.0F;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + BATTERY_X,
                    topPos + BATTERY_Y,
                    u,
                    16.0F,
                    BATTERY_W,
                    BATTERY_H,
                    256,
                    256);
        }
        if (bomb.started) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + START_X,
                    topPos + START_Y,
                    176.0F,
                    26.0F,
                    START_SIZE,
                    START_SIZE,
                    256,
                    256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BalefireNukeBlockEntity bomb = menu.blockEntity();

        graphics.text(font, title, (imageWidth - font.width(title)) / 2, 6, -12566464, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 96 + 2, -12566464, false);

        if (bomb.getBattery() != BalefireNukeBlockEntity.BATTERY_NONE) {
            Component clock = Component.literal(bomb.getMinutes() + ":" + bomb.getSeconds());
            graphics.pose().pushMatrix();
            graphics.pose().scale(CLOCK_SCALE, CLOCK_SCALE);
            graphics.text(
                    font,
                    clock,
                    (int) ((CLOCK_CENTRE - font.width(clock) / 2F) / CLOCK_SCALE),
                    (int) (CLOCK_Y / CLOCK_SCALE),
                    0xFFFF0000,
                    false);
            graphics.pose().popMatrix();
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

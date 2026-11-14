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

import com.hbm.blockentity.network.CraneRouterBlockEntity;
import com.hbm.client.Keybinds;
import com.hbm.inventory.container.CraneRouterMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.CraneRouterCycleLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class CraneRouterScreen extends InfoContainerScreen<CraneRouterMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_crane_router.png");

    private static final int BUTTON_X = 7;
    private static final int BUTTON_Y = 16;
    private static final int BUTTON_SIZE = 18;
    private static final int BUTTON_COLUMN_STEP = 222;
    private static final int BUTTON_ROW_STEP = 26;

    public CraneRouterScreen(CraneRouterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 201);
        this.titleLabelY = 5;
        this.inventoryLabelX = 47;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static int buttonX(int column) {
        return BUTTON_X + column * BUTTON_COLUMN_STEP;
    }

    private static int buttonY(int row) {
        return BUTTON_Y + row * BUTTON_ROW_STEP;
    }

    private static List<Component> modeLines(int mode) {
        return switch (mode) {
            case CraneRouterBlockEntity.MODE_WHITELIST ->
                    List.of(
                            Component.translatable("desc.gui.craneRouter.whitelist"),
                            Component.translatable("desc.gui.craneRouter.routeIfFilterMatches"));
            case CraneRouterBlockEntity.MODE_BLACKLIST ->
                    List.of(
                            Component.translatable("desc.gui.craneRouter.blacklist"),
                            Component.translatable("desc.gui.craneRouter.routeIfFilterDoesn"));
            case CraneRouterBlockEntity.MODE_WILDCARD ->
                    List.of(
                            Component.translatable("desc.gui.craneRouter.wildcard"),
                            Component.translatable("desc.gui.craneRouter.routeIfNoOther"));
            default -> List.of(Component.translatable("desc.gui.craneRouter.off"));
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        CraneRouterBlockEntity router = menu.blockEntity();

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, 256, 93, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 39, 93, 39, 93, 176, 108, 256, 256);

        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                int index = column * 3 + row;
                int x = buttonX(column);
                int y = buttonY(row);

                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x,
                        y,
                        238,
                        93 + router.modes[index] * 18,
                        BUTTON_SIZE,
                        BUTTON_SIZE,
                        256,
                        256);
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        x,
                        y,
                        BUTTON_SIZE,
                        BUTTON_SIZE,
                        modeLines(router.modes[index]));
            }
        }

        if (Keybinds.DIAGNOSTICS.isDown()) {
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.getSlot(i);
                graphics.text(
                        font, Component.literal(String.valueOf(i)), slot.x + 2, slot.y, -1, true);
                graphics.text(
                        font,
                        Component.literal(String.valueOf(slot.getContainerSlot())),
                        slot.x + 2,
                        slot.y + 8,
                        0xFFFF8080,
                        true);
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> slotTooltip(Slot slot) {
        if (slot.index >= CraneRouterBlockEntity.SLOT_COUNT) return List.of();
        return patternModeLines(
                menu.blockEntity()
                        .patterns[slot.index / CraneRouterBlockEntity.FILTERS_PER_SIDE]
                        .mode(slot.index % CraneRouterBlockEntity.FILTERS_PER_SIDE));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();

        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                if (!checkClick(mx, my, buttonX(column), buttonY(row), BUTTON_SIZE, BUTTON_SIZE))
                    continue;

                PacketWire.sendToServer(
                        CraneRouterCycleLayout.encode(menu.containerId, column * 3 + row));
                playClick();
            }
        }

        return super.mouseClicked(event, doubleClick);
    }
}

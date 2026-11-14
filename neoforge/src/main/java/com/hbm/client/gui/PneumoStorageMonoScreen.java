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

import com.hbm.blockentity.network.pneumatic.PneumaticTubeBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumoStorageMonoBlockEntity;
import com.hbm.inventory.container.PneumoStorageMonoMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PneumaticPressureLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class PneumoStorageMonoScreen extends InfoContainerScreen<PneumoStorageMonoMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/storage/gui_pneumatic_mono.png");

    public PneumoStorageMonoScreen(
            PneumoStorageMonoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 181);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 176 / 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PneumoStorageMonoBlockEntity storage = menu.blockEntity();

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        for (int i = 0; i < PneumoStorageMonoBlockEntity.SLOT_COUNT; i++) {
            if (storage.getItem(i).isEmpty()) continue;
            int bar = storage.amounts[i] * 124 / PneumoStorageMonoBlockEntity.CAPACITY;
            if (bar > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        44,
                        17 + i * 18,
                        0,
                        181,
                        bar,
                        16,
                        256,
                        256);
            }
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                174 + 4 * (storage.airTank.getPressure() - 1),
                36,
                200,
                0,
                4,
                8,
                256,
                256);
        SmoothGaugeElement.draw(
                graphics,
                184,
                25,
                (double) storage.airTank.getFill() / storage.airTank.getMaxFill(),
                5,
                2,
                1,
                0xFFCA6C43,
                0xFFAB4223);

        for (int k = 0; k < PneumoStorageMonoBlockEntity.SLOT_COUNT; k++) {
            if (storage.getItem(k).isEmpty()) continue;
            int amount = storage.amounts[k];
            String percent =
                    " ("
                            + (((int) (amount * 1000D / PneumoStorageMonoBlockEntity.CAPACITY))
                                    / 10D)
                            + "%)";
            graphics.text(
                    font,
                    Component.literal(String.format(Locale.ROOT, "%,d", amount) + percent),
                    50,
                    22 + k * 18,
                    0xFF000000,
                    false);
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                174,
                36,
                20,
                8,
                List.of(
                        Component.translatable(
                                "desc.shared.compressor", storage.airTank.getPressure()),
                        Component.translatable(
                                "desc.shared.maxRange",
                                PneumaticTubeBlockEntity.rangeForPressure(
                                        storage.airTank.getPressure()))));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        if (checkClick((int) x, (int) y, 174, 36, 20, 8)) {
            PacketWire.sendToServer(PneumaticPressureLayout.encode(menu.containerId));
            playClick();
        }
        return super.mouseClicked(event, doubleClick);
    }
}

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

import com.hbm.inventory.container.NukeSoliniumMenu;
import com.hbm.items.ModItems;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public class NukeSoliniumScreen extends InfoContainerScreen<NukeSoliniumMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/soliniumschematic.png");

    private static final int[][] PARTS = {
        {24, 84, 0, 222, 22, 14},
        {46, 84, 22, 222, 18, 14},
        {76, 84, 52, 222, 18, 14},
        {94, 84, 70, 222, 22, 14},
        {64, 84, 40, 222, 12, 28},
        {24, 98, 0, 236, 22, 14},
        {46, 98, 22, 236, 18, 14},
        {76, 98, 52, 236, 18, 14},
        {94, 98, 70, 236, 22, 14}
    };

    public NukeSoliniumScreen(NukeSoliniumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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

        for (int slot = 0; slot < PARTS.length; slot++) {
            Item item =
                    switch (slot) {
                        case 0, 3, 5, 8 -> ModItems.SOLINIUM_IGNITER.get();
                        case 4 -> ModItems.SOLINIUM_CORE.get();
                        default -> ModItems.SOLINIUM_PROPELLANT.get();
                    };
            if (!menu.part(slot).is(item)) continue;
            int[] r = PARTS[slot];
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    r[0],
                    r[1],
                    r[2],
                    r[3],
                    r[4],
                    r[5],
                    256,
                    256);
        }

        if (menu.isReady()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 134, 90, 176.0F, 0.0F, 16, 16, 256, 256);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

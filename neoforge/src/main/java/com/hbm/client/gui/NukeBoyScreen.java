/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.inventory.container.NukeBoyMenu;
import com.hbm.items.ModItems;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public class NukeBoyScreen extends InfoContainerScreen<NukeBoyMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/lilboyschematic.png");

    public NukeBoyScreen(NukeBoyMenu menu, Inventory playerInventory, Component title) {
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

        if (menu.isReady()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 142, 90, 176.0F, 0.0F, 16, 16, 256, 256);
        }

        part(graphics, 0, ModItems.BOY_SHIELDING.get(), 27, 87, 176, 16, 21, 22);
        part(graphics, 1, ModItems.BOY_TARGET.get(), 27, 89, 176, 38, 21, 18);
        part(graphics, 2, ModItems.BOY_BULLET.get(), 74, 94, 176, 57, 19, 8);
        part(graphics, 3, ModItems.BOY_PROPELLANT.get(), 92, 95, 176, 66, 12, 6);
        part(graphics, 4, ModItems.BOY_IGNITER.get(), 107, 91, 176, 75, 16, 14);

        drawInfoPanel(graphics, -16, 16, 16, 16, 2);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                -16,
                16,
                16,
                16,
                -8,
                32,
                lineArray("desc.gui.nukeBoy.desc"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void part(
            GuiGraphicsExtractor graphics,
            int slot,
            Item item,
            int x,
            int y,
            int u,
            int v,
            int w,
            int h) {
        if (!menu.part(slot).is(item)) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, w, h, 256, 256);
    }
}

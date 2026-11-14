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

import com.hbm.inventory.container.CartDestroyerMenu;
import com.hbm.util.GameTime;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class CartDestroyerScreen extends InfoContainerScreen<CartDestroyerMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/vehicles/gui_destroyer.png");

    public CartDestroyerScreen(CartDestroyerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 96 + 4;
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

        long now = GameTime.now();
        int index = (int) (now % 1000 / 128);
        if (index == 1 || index == 7)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 66, 35, 0, 166, 44, 16, 256, 256);
        if (index == 2 || index == 6)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 66, 35, 0, 182, 44, 16, 256, 256);
        if (index == 3 || index == 5)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 66, 35, 0, 198, 44, 16, 256, 256);
        if (index == 4)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 66, 35, 0, 214, 44, 16, 256, 256);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}

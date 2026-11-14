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

import com.hbm.inventory.container.NukeGadgetMenu;
import com.hbm.items.ModItems;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class NukeGadgetScreen extends InfoContainerScreen<NukeGadgetMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/gadgetschematic.png");

    public NukeGadgetScreen(NukeGadgetMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
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

        if (lens(1))
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 19, 176.0F, 0.0F, 24, 24, 256, 256);
        if (lens(2))
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 106, 19, 200.0F, 0.0F, 24, 24, 256, 256);
        if (lens(3))
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 43, 176.0F, 24.0F, 24, 24, 256, 256);
        if (lens(4))
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    106,
                    43,
                    200.0F,
                    24.0F,
                    24,
                    24,
                    256,
                    256);
        if (menu.isReady()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    134,
                    35,
                    176.0F,
                    48.0F,
                    16,
                    16,
                    256,
                    256);
        }

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
                lineArray("desc.gui.nukeGadget.desc"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private boolean lens(int slot) {
        return menu.part(slot).is(ModItems.EARLY_EXPLOSIVE_LENSES.get());
    }
}

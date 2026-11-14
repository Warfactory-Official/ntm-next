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

package com.hbm.client;

import com.hbm.extprop.PlayerState;
import com.hbm.render.util.ScreenOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public final class ShieldHudRenderer {

    private ShieldHudRenderer() {}

    public static boolean shown() {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        return player != null
                && minecraft.gameMode != null
                && minecraft.gameMode.canHurtPlayer()
                && PlayerState.getOrCreate(player).shieldCapacity(player) > 0F;
    }

    public static void render(GuiGraphicsExtractor graphics, int top) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        PlayerState props = PlayerState.getOrCreate(player);
        float maximum = props.shieldCapacity(player);
        int left = graphics.guiWidth() / 2 - 91;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ScreenOverlayRenderer.misc,
                left,
                top,
                146,
                0,
                81,
                9,
                256,
                256);
        int fill = (int) Math.ceil(props.shield * 79 / maximum);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ScreenOverlayRenderer.misc,
                left + 1,
                top,
                147,
                9,
                fill,
                9,
                256,
                256);
        String label = Double.toString(((int) (props.shield * 10F)) / 10D);
        int center = left + 40 - minecraft.font.width(label) / 2;
        graphics.text(minecraft.font, label, center + 1, top + 1, 0xFF000000, false);
        graphics.text(minecraft.font, label, center - 1, top + 1, 0xFF000000, false);
        graphics.text(minecraft.font, label, center, top, 0xFF000000, false);
        graphics.text(minecraft.font, label, center, top + 2, 0xFF000000, false);
        graphics.text(minecraft.font, label, center, top + 1, 0xFFFFFF80, false);
    }
}

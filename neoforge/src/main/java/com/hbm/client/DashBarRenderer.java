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

package com.hbm.client;

import com.hbm.extprop.PlayerState;
import com.hbm.items.armor.ArmorDash;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public final class DashBarRenderer {

    private static final Identifier MISC = RegistryUtil.id("textures/misc/overlay_misc.png");

    private static final int WIDTH = 30;
    private static final int HEIGHT = 10;
    private static final int PER_ROW = 3;
    private static final float V_RED = 18F;
    private static final float V_NORMAL = 28F;
    private static final float V_PARTIAL = 38F;
    private static final float V_EMPTY = 48F;
    private static final float V_FLASH = 58F;
    private static final float U_BAR = 76F;
    private static final float FADE_STEP = 0.04F;
    private static final int FLASH_AT = ArmorDash.PER_DASH - 3;

    private static float fade;
    private static int fadeBar = -1;

    private DashBarRenderer() {}

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerState props = PlayerState.getOrCreate(player);
        int dashes = props.dashCount;
        if (dashes <= 0) {
            fade = 0F;
            fadeBar = -1;
            return;
        }

        int stamina = props.stamina;
        int filled = stamina / ArmorDash.PER_DASH;
        int partial = stamina % ArmorDash.PER_DASH;

        int posX = 16;
        int posY = mc.getWindow().getGuiScaledHeight() - 42;

        g.blit(RenderPipelines.GUI_TEXTURED, MISC, posX - 10, posY, 107F, 18F, 7, HEIGHT, 256, 256);

        if (filled < dashes && partial >= FLASH_AT) {
            fade = 1F;
            fadeBar = filled;
        }

        for (int bar = 0; bar < dashes; bar++) {
            int x = posX + (WIDTH + 2) * (bar % PER_ROW);
            int y = posY - 12 * (bar / PER_ROW);

            g.blit(
                    RenderPipelines.GUI_TEXTURED,
                    MISC,
                    x,
                    y,
                    U_BAR,
                    V_EMPTY,
                    WIDTH,
                    HEIGHT,
                    256,
                    256);

            float v = V_NORMAL;
            int fill = WIDTH;
            if (filled < bar) {
                v = V_EMPTY;
            } else if (filled == bar) {
                v = bar == 0 ? V_RED : V_PARTIAL;
                fill = (int) (partial * (WIDTH / (float) ArmorDash.PER_DASH));
            }
            if (fill > 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, MISC, x, y, U_BAR, v, fill, HEIGHT, 256, 256);
            }

            if (fade > 0F && bar == fadeBar) {
                g.blit(
                        RenderPipelines.GUI_TEXTURED,
                        MISC,
                        x,
                        y,
                        U_BAR,
                        V_FLASH,
                        WIDTH,
                        HEIGHT,
                        256,
                        256,
                        ARGB.color((int) (fade * 255F), 255, 255, 255));
            }
        }

        if (fade > 0F) fade = Math.max(0F, fade - FADE_STEP);
    }
}

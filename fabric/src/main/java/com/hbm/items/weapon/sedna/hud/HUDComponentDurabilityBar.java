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

package com.hbm.items.weapon.sedna.hud;

import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.render.util.ScreenOverlayRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HUDComponentDurabilityBar implements GunHudWidget {

    protected boolean mirrored = false;

    public HUDComponentDurabilityBar() {
        this(false);
    }

    public HUDComponentDurabilityBar(boolean mirror) {
        this.mirrored = mirror;
    }

    @Override
    public int height() {
        return 5;
    }

    @Override
    public void draw(
            GuiGraphicsExtractor graphics,
            Player player,
            ItemStack stack,
            int bottomOffset,
            int gunIndex) {

        int pX = graphics.guiWidth() / 2 + (mirrored ? -(62 + 36 + 52) : (62 + 36));
        int pZ = graphics.guiHeight() - 21;

        GunBaseNTItem gun = (GunBaseNTItem) stack.getItem();
        int dura =
                (int)
                        (50
                                * GunBaseNTItem.getWear(stack, gunIndex)
                                / gun.getConfig(stack, gunIndex).getDurability(stack));

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ScreenOverlayRenderer.misc,
                pX,
                pZ + 16,
                94F,
                0F,
                52,
                3,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ScreenOverlayRenderer.misc,
                pX + 1,
                pZ + 16,
                95F,
                3F,
                50 - dura,
                3,
                256,
                256);
    }
}

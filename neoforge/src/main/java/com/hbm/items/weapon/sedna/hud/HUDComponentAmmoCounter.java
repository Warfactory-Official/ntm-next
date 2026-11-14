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
import com.hbm.items.weapon.sedna.hud.GunHudWidget;
import com.hbm.items.weapon.sedna.mags.Magazine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HUDComponentAmmoCounter implements GunHudWidget {

    protected int receiver;
    protected boolean mirrored;
    protected boolean noCounter;

    public HUDComponentAmmoCounter(int receiver) {
        this.receiver = receiver;
    }

    public HUDComponentAmmoCounter mirror() {
        this.mirrored = true;
        return this;
    }

    public HUDComponentAmmoCounter noCounter() {
        this.noCounter = true;
        return this;
    }

    @Override
    public int height() {
        return 17;
    }

    @Override
    public void draw(
            GuiGraphicsExtractor graphics,
            Player player,
            ItemStack stack,
            int bottomOffset,
            int gunIndex) {

        int pX =
                graphics.guiWidth() / 2
                        + (mirrored ? -(62 + 36 + 52) : (62 + 36))
                        + (noCounter ? 14 : 0);
        int pZ = graphics.guiHeight() - bottomOffset - 18;
        GunBaseNTItem gun = (GunBaseNTItem) stack.getItem();
        Magazine<?> mag =
                gun.getConfig(stack, gunIndex)
                        .getReceivers(stack)
                        .get(this.receiver)
                        .getMagazine(stack);

        if (!noCounter)
            graphics.text(
                    Minecraft.getInstance().font,
                    mag.getAmmoStateForHud(stack, player),
                    pX + 17,
                    pZ + 6,
                    0xFFFFFFFF,
                    false);

        ItemStack icon = mag.getIconForHud(stack, player);
        if (icon != null && !icon.isEmpty()) graphics.item(icon, pX, pZ);
    }
}

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

package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.particle.casing.Casing;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MagazineInfinite implements Magazine<BulletConfig> {

    public BulletConfig type;

    public MagazineInfinite(BulletConfig type) {
        this.type = type;
    }

    @Override
    public BulletConfig getType(ItemStack stack, Container inventory) {
        return this.type;
    }

    @Override
    public void setType(ItemStack stack, BulletConfig type) {}

    @Override
    public int getCapacity(ItemStack stack) {
        return 9999;
    }

    @Override
    public int getAmount(ItemStack stack, Container inventory) {
        return 9999;
    }

    @Override
    public void setAmount(ItemStack stack, int amount) {}

    @Override
    public void consumeAmmo(ItemStack stack, Container inventory, int amount) {}

    @Override
    public boolean canReload(ItemStack stack, Container inventory) {
        return false;
    }

    @Override
    public void initNewType(ItemStack stack, Container inventory) {}

    @Override
    public void reloadAction(ItemStack stack, Container inventory) {}

    @Override
    public ItemStack getIconForHud(ItemStack stack, Player player) {
        return ItemStack.EMPTY;
    }

    @Override
    public String getAmmoStateForHud(ItemStack stack, Player player) {
        return "∞";
    }

    @Override
    public Casing getCasing(ItemStack stack, Container inventory) {
        return this.type.casing;
    }

    @Override
    public void setAmountBeforeReload(ItemStack stack, int amount) {}

    @Override
    public int getAmountBeforeReload(ItemStack stack) {
        return 9999;
    }

    @Override
    public void setAmountAfterReload(ItemStack stack, int amount) {}

    @Override
    public int getAmountAfterReload(ItemStack stack) {
        return 9999;
    }
}

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

import com.hbm.items.machine.FluidIconItem;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.particle.casing.Casing;
import java.util.function.Supplier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class MagazineLiquidEngine implements Magazine<Fluid> {

    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";
    private final Supplier<Fluid[]> acceptedSupplier;
    public int index;
    public int capacity;
    private Fluid[] acceptedTypes;

    public MagazineLiquidEngine(int index, int capacity, Supplier<Fluid[]> acceptedTypes) {
        this.index = index;
        this.capacity = capacity;
        this.acceptedSupplier = acceptedTypes;
    }

    public static int getMagCount(ItemStack stack, int index) {
        return GunBaseNTItem.getValueInt(stack, KEY_MAG_COUNT + index);
    }

    public static void setMagCount(ItemStack stack, int index, int value) {
        GunBaseNTItem.setValueInt(stack, KEY_MAG_COUNT + index, value);
    }

    public Fluid[] accepted() {
        if (acceptedTypes == null) acceptedTypes = acceptedSupplier.get();
        return acceptedTypes;
    }

    @Override
    public Fluid getType(ItemStack stack, Container inventory) {
        return accepted()[0];
    }

    @Override
    public void setType(ItemStack stack, Fluid type) {}

    @Override
    public int getCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public void consumeAmmo(ItemStack stack, Container inventory, int amount) {
        this.setAmount(stack, Math.max(this.getAmount(stack, inventory) - amount, 0));
    }

    @Override
    public int getAmount(ItemStack stack, Container inventory) {
        return getMagCount(stack, index);
    }

    @Override
    public void setAmount(ItemStack stack, int amount) {
        setMagCount(stack, index, amount);
    }

    @Override
    public boolean canReload(ItemStack stack, Container inventory) {
        return false;
    }

    @Override
    public void initNewType(ItemStack stack, Container inventory) {}

    @Override
    public void reloadAction(ItemStack stack, Container inventory) {}

    @Override
    public Casing getCasing(ItemStack stack, Container inventory) {
        return null;
    }

    @Override
    public ItemStack getIconForHud(ItemStack stack, Player player) {
        return FluidIconItem.make(this.getType(stack, player.getInventory()));
    }

    @Override
    public String getAmmoStateForHud(ItemStack stack, Player player) {
        return getAmount(stack, player.getInventory()) + "/" + this.capacity + "mB";
    }

    @Override
    public void setAmountBeforeReload(ItemStack stack, int amount) {
        GunBaseNTItem.setValueInt(stack, KEY_MAG_PREV + index, amount);
    }

    @Override
    public int getAmountBeforeReload(ItemStack stack) {
        return GunBaseNTItem.getValueInt(stack, KEY_MAG_PREV + index);
    }

    @Override
    public void setAmountAfterReload(ItemStack stack, int amount) {
        GunBaseNTItem.setValueInt(stack, KEY_MAG_AFTER + index, amount);
    }

    @Override
    public int getAmountAfterReload(ItemStack stack) {
        return GunBaseNTItem.getValueInt(stack, KEY_MAG_AFTER + index);
    }
}

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

package com.hbm.items.weapon.sedna.impl;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.machine.FluidContainerItem;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.items.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class GunDrillItem extends GunBaseNTItem implements FluidContainerItem, EnergyItem {

    public GunDrillItem(WeaponQuality quality, Properties properties, GunConfig.Builder... cfg) {
        super(quality, properties, cfg);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        int level = XFactoryDrill.getModdableHarvestLevel(stack, 2);
        if (level >= 4) return !state.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL);
        if (level == 3) return !state.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
        if (level == 2) return !state.is(BlockTags.INCORRECT_FOR_IRON_TOOL);
        if (level == 1) return !state.is(BlockTags.INCORRECT_FOR_STONE_TOOL);
        return !state.is(BlockTags.INCORRECT_FOR_WOODEN_TOOL);
    }

    private MagazineLiquidEngine engine(ItemStack stack) {
        Magazine mag = this.getConfig(stack, 0).getReceivers(stack).get(0).getMagazine(stack);
        return mag instanceof MagazineLiquidEngine e ? e : null;
    }

    private MagazineElectricEngine electricEngine(ItemStack stack) {
        Magazine mag = this.getConfig(stack, 0).getReceivers(stack).get(0).getMagazine(stack);
        return mag instanceof MagazineElectricEngine e ? e : null;
    }

    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        if (engine == null) return false;
        for (Fluid acc : engine.accepted()) if (type == acc) return true;
        return false;
    }

    @Override
    public boolean machineFillable() {
        return true;
    }

    @Override
    public Fluid firstFluidType(ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        return engine == null ? NTMFluids.NONE : engine.getType(stack, null);
    }

    @Override
    public int capacity(ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        return engine == null ? 0 : engine.getCapacity(stack);
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        if (engine == null) return EMPTY_CONTENT;
        int fill = engine.getAmount(stack, null);
        if (fill <= 0) return EMPTY_CONTENT;
        return new FluidStackNTM(engine.getType(stack, null), fill, 0);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        MagazineLiquidEngine engine = engine(stack);
        if (engine != null)
            engine.setAmount(stack, (int) Math.min(content.amount(), capacity(stack)));
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (!acceptsFluid(type, stack)) return 0;
        MagazineLiquidEngine engine = engine(stack);
        int capacity = engine.getCapacity(stack);
        int fill = Math.clamp(engine.getAmount(stack, null), 0, capacity);
        int toFill = Math.clamp(amount, 0, Math.min(50, capacity - fill));
        engine.setAmount(stack, fill + toFill);
        return toFill;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        return 0;
    }

    @Override
    public void addCharge(ItemStack stack, long amount) {
        MagazineElectricEngine engine = electricEngine(stack);
        if (engine != null)
            engine.setAmount(
                    stack, Math.min(engine.capacity, engine.getAmount(stack, null) + (int) amount));
    }

    @Override
    public void setCharge(ItemStack stack, long amount) {
        MagazineElectricEngine engine = electricEngine(stack);
        if (engine != null) engine.setAmount(stack, (int) amount);
    }

    @Override
    public void removeCharge(ItemStack stack, long amount) {
        MagazineElectricEngine engine = electricEngine(stack);
        if (engine != null)
            engine.setAmount(stack, Math.max(0, engine.getAmount(stack, null) - (int) amount));
    }

    @Override
    public long getCharge(ItemStack stack) {
        MagazineElectricEngine engine = electricEngine(stack);
        return engine == null ? 0L : engine.getAmount(stack, null);
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        MagazineElectricEngine engine = electricEngine(stack);
        return engine == null ? 0L : engine.getCapacity(stack);
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return 50_000L;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0L;
    }
}

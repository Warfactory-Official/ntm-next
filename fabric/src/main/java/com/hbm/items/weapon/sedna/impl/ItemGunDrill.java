// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.impl;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.config.GunVisualConfig;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class ItemGunDrill extends ItemGunBaseNT implements IFluidContainerItem, IBatteryItem {

    public ItemGunDrill(WeaponQuality quality, Properties properties, GunConfig... cfg) {
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
        IMagazine mag = this.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        return mag instanceof MagazineLiquidEngine e ? e : null;
    }

    private MagazineElectricEngine electricEngine(ItemStack stack) {
        IMagazine mag = this.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
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
        int fill = engine.getAmount(stack, null);
        int toFill = Math.min(amount, 50);
        toFill = Math.min(toFill, engine.getCapacity(stack) - fill);
        engine.setAmount(stack, fill + toFill);
        return toFill;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        return 0;
    }

    @Override
    public void chargeBattery(ItemStack stack, long amount) {
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
    public void dischargeBattery(ItemStack stack, long amount) {
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

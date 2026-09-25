// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.particle.SpentCasing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class MagazineFluid implements IMagazine<Fluid> {

    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_TYPE = "magtype";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";

    public int index;
    public int capacity;

    public MagazineFluid(int index, int capacity) {
        this.index = index;
        this.capacity = capacity;
    }

    public static int getMagType(ItemStack stack, int index) {
        return ItemGunBaseNT.getValueInt(stack, KEY_MAG_TYPE + index);
    }

    public static void setMagType(ItemStack stack, int index, int value) {
        ItemGunBaseNT.setValueInt(stack, KEY_MAG_TYPE + index, value);
    }

    public static int getMagCount(ItemStack stack, int index) {
        return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index);
    }

    public static void setMagCount(ItemStack stack, int index, int value) {
        ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value);
    }

    @Override
    public Fluid getType(ItemStack stack, Container inventory) {
        return BuiltInRegistries.FLUID.byId(getMagType(stack, index));
    }

    @Override
    public void setType(ItemStack stack, Fluid type) {
        setMagType(stack, index, BuiltInRegistries.FLUID.getId(type));
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public void useUpAmmo(ItemStack stack, Container inventory, int amount) {
        this.setAmount(stack, this.getAmount(stack, inventory) - amount);
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
    public SpentCasing getCasing(ItemStack stack, Container inventory) {
        return null;
    }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, Player player) {
        return ItemFluidIcon.make(getType(stack, player.getInventory()));
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return getAmount(stack, player.getInventory()) + "mB";
    }

    @Override
    public void setAmountBeforeReload(ItemStack stack, int amount) {
        ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount);
    }

    @Override
    public int getAmountBeforeReload(ItemStack stack) {
        return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index);
    }

    @Override
    public void setAmountAfterReload(ItemStack stack, int amount) {
        ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount);
    }

    @Override
    public int getAmountAfterReload(ItemStack stack) {
        return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index);
    }
}

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

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.ChemicalEntity;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.machine.FluidContainerItem;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public class GunChemthrowerItem extends GunBaseNTItem implements FluidContainerItem {

    public static final int CONSUMPTION = 3;
    public static final int transferSpeed = 50;
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_CAN_FIRE =
            (stack, ctx) -> {
                return ctx.config()
                                .getReceivers(stack)
                                .get(0)
                                .getMagazine(stack)
                                .getAmount(stack, ctx.inventory())
                        >= CONSUMPTION;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                Player player = ctx.getPlayer();
                int index = ctx.configIndex();
                GunBaseNTItem.playAnimation(player, stack, GunAnimation.CYCLE, ctx.configIndex());

                Receiver primary = ctx.config().getReceivers(stack).get(0);
                Magazine mag = primary.getMagazine(stack);

                Vec3 offset = primary.getProjectileOffset(stack);
                double forwardOffset = offset.x;
                double heightOffset = offset.y;
                double sideOffset = offset.z;

                ChemicalEntity chem =
                        new ChemicalEntity(
                                ModEntities.CHEMICAL.get(),
                                entity.level(),
                                entity,
                                sideOffset,
                                heightOffset,
                                forwardOffset);
                chem.setFluid((Fluid) mag.getType(stack, ctx.inventory()));
                entity.level().addFreshEntity(chem);

                mag.consumeAmmo(stack, ctx.inventory(), CONSUMPTION);
                GunBaseNTItem.setWear(
                        stack,
                        index,
                        Math.min(
                                GunBaseNTItem.getWear(stack, index) + 1F,
                                ctx.config().getDurability(stack)));
            };

    public GunChemthrowerItem(
            WeaponQuality quality, Properties properties, GunConfig.Builder... cfg) {
        super(quality, properties, cfg);
    }

    public static Fluid getMagType(ItemStack stack) {
        return MagazineFluid.getMagType(stack, 0);
    }

    public static void setMagType(ItemStack stack, Fluid type) {
        MagazineFluid.setMagType(stack, 0, type);
    }

    public static int getMagCount(ItemStack stack) {
        return GunBaseNTItem.getValueInt(stack, MagazineFluid.KEY_MAG_COUNT + 0);
    }

    public static void setMagCount(ItemStack stack, int value) {
        GunBaseNTItem.setValueInt(stack, MagazineFluid.KEY_MAG_COUNT + 0, value);
    }

    @Override
    public boolean machineFillable() {
        return true;
    }

    @Override
    public Fluid firstFluidType(ItemStack stack) {
        return getMagType(stack);
    }

    @Override
    public int capacity(ItemStack stack) {
        return this.getConfig(stack, 0)
                .getReceivers(stack)
                .get(0)
                .getMagazine(stack)
                .getCapacity(stack);
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        int fill = getMagCount(stack);
        if (fill <= 0) return EMPTY_CONTENT;
        return new FluidStackNTM(getMagType(stack), fill, 0);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        setMagType(stack, content.type());
        setMagCount(stack, (int) Math.min(content.amount(), capacity(stack)));
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (getMagType(stack) != type && getMagCount(stack) != 0) return 0;
        if (getMagCount(stack) == 0) setMagType(stack, type);

        int fill = getMagCount(stack);
        int req = capacity(stack) - fill;
        int toFill = Math.min(amount, req);
        toFill = Math.min(toFill, transferSpeed);
        setMagCount(stack, fill + toFill);
        return toFill;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        if (getMagType(stack) != type) return 0;
        int fill = getMagCount(stack);
        int toUnload = Math.min(fill, amount);
        toUnload = Math.min(toUnload, transferSpeed);
        setMagCount(stack, fill - toUnload);
        return toUnload;
    }
}

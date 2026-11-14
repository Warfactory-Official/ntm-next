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

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.config.GunVisualConfig;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.extprop.LivingHazards;
import com.hbm.items.ModItems;
import com.hbm.items.variant.CasingType;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.ModSounds;
import com.hbm.util.RegistryUtil;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class XFactory556mm {

    public static final Identifier scope = RegistryUtil.id("textures/misc/scope_bolt.png");

    public static BulletConfig r556_sp;
    public static BulletConfig r556_fmj;
    public static BulletConfig r556_jhp;
    public static BulletConfig r556_ap;

    public static BulletConfig r556_inc_sp;
    public static BulletConfig r556_inc_fmj;
    public static BulletConfig r556_inc_jhp;
    public static BulletConfig r556_inc_ap;

    public static BiConsumer<BulletBaseMK4Entity, HitResult> INCENDIARY =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    LivingHazards data = LivingHazards.getOrCreate(living);
                    if (data.phosphorus < 300) data.phosphorus = 300;
                }
            };
    public static Function<ItemStack, String> LAMBDA_NAME_G3 =
            (stack) -> {
                if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER)
                        && XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_STOCK)
                        && XWeaponModManager.hasUpgrade(
                                stack, 0, XWeaponModManager.ID_FURNITURE_BLACK)
                        && XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE))
                    return stack.getItem().getDescriptionId() + "_infiltrator";
                if (!XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_STOCK)
                        && XWeaponModManager.hasUpgrade(
                                stack, 0, XWeaponModManager.ID_FURNITURE_GREEN))
                    return stack.getItem().getDescriptionId() + "_a3";
                return null;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE =
            (stack, ctx) -> {
                Lego.handleStandardSmoke(ctx.entity(), stack, 1500, 0.075D, 1.1D, 0);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STG77_DECIDER =
            (stack, ctx) -> {
                int index = ctx.configIndex();
                GunState lastState = GunBaseNTItem.getState(stack, index);
                GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
                GunStateDecider.deciderStandardClearJam(stack, lastState, index);
                GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
                GunStateDecider.deciderAutoRefire(
                        stack,
                        ctx,
                        lastState,
                        0,
                        index,
                        () -> {
                            return GunBaseNTItem.getSecondary(stack, index);
                        });
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_G3 =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ZEBRA =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_STG = (stack, ctx) -> {};

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_G3_ANIMS =
            (stack, type) -> {
                boolean empty =
                        ((GunBaseNTItem) stack.getItem())
                                        .getConfig(stack, 0)
                                        .getReceivers(stack)
                                        .get(0)
                                        .getMagazine(stack)
                                        .getAmount(
                                                stack, ClientPlayerAccess.player().getInventory())
                                <= 0;
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(45, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_FULL));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "BOLT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 20)
                                                .to(0, 0, -4.5, 40)
                                                .to(0, 0, 0, 40))
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(
                                                        0,
                                                        0,
                                                        (GunBaseNTItem.getIsAiming(stack)
                                                                        || !XWeaponModManager
                                                                                .hasUpgrade(
                                                                                        stack,
                                                                                        0,
                                                                                        XWeaponModManager
                                                                                                .ID_NO_STOCK))
                                                                ? -0.25
                                                                : -0.75,
                                                        25,
                                                        IType.SIN_DOWN)
                                                .to(0, 0, 0, 75, IType.SIN_FULL));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "BOLT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, -0.3125, 100)
                                                .stay(25)
                                                .to(0, 0, -2.75, 130)
                                                .stay(50)
                                                .to(0, 0, -2.4375, 50)
                                                .to(0, 0, 0, 85))
                                .addBus(
                                        "PLUG",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .stay(125)
                                                .to(0, 0, -2.4375, 130)
                                                .stay(100)
                                                .to(0, 0, 0, 85))
                                .addBus(
                                        "HANDLE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, 90, 0, 100)
                                                .stay(25)
                                                .stay(180)
                                                .to(0, 0, 0, 50))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 400)
                                                .to(-1, 0, 0, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, -8, 0, 250, IType.SIN_UP)
                                                .to(0, -8, 0, 1050)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "BOLT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, -0.3125, 100)
                                                .stay(10)
                                                .to(0, 0, -3.25, 200)
                                                .stayUntil(1875)
                                                .to(0, 0, -2.9375, 50)
                                                .to(0, 0, 0, 100))
                                .addBus(
                                        "PLUG",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 310)
                                                .to(0, 0, -2.9375, 200)
                                                .stayUntil(1925)
                                                .to(0, 0, 0, 100))
                                .addBus(
                                        "HANDLE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(0, 90, 0, 100)
                                                .stay(210)
                                                .to(0, 90, 45, 75)
                                                .stayUntil(1775)
                                                .to(0, 90, 0, 100)
                                                .to(0, 0, 0, 50))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 750)
                                                .to(-25, 0, 0, 500, IType.SIN_FULL)
                                                .stayUntil(1550)
                                                .to(-26, 0, 0, 100, IType.SIN_DOWN)
                                                .to(-25, 0, 0, 100, IType.SIN_FULL)
                                                .stayUntil(2000)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .to(empty ? 1 : 0, 0, 0, 0)
                                                .to(0, 0, 0, 1000));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, -1, 0, 150)
                                                .to(2, -1, 0, 150)
                                                .to(2, 8, 0, 350, IType.SIN_DOWN)
                                                .to(2, -2, 0, 350, IType.SIN_UP)
                                                .to(2, -1, 0, 50)
                                                .to(2, -1, 0, 100)
                                                .to(0, -1, 0, 150, IType.SIN_FULL)
                                                .to(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "SPEEN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 300)
                                                .to(0, 360, 360, 700))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1450)
                                                .to(-2, 0, 0, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence().to(empty ? 1 : 0, 0, 0, 0));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 1250)
                                                .to(0, 0, 0, 350, IType.SIN_FULL))
                                .addBus(
                                        "BOLT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1000)
                                                .to(0, 0, -3.25, 150)
                                                .to(0, 0, 0, 100)
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, -3.25, 150)
                                                .to(0, 0, 0, 100))
                                .addBus(
                                        "PLUG",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1000)
                                                .to(0, 0, -3.25, 150)
                                                .to(0, 0, 0, 100)
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, -3.25, 150)
                                                .to(0, 0, 0, 100));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_STG77_ANIMS =
            (stack, type) -> {
                if (GunVisualConfig.legacyAnimations) {
                    switch (type) {
                        case EQUIP:
                            return new BusAnimation()
                                    .addBus(
                                            "EQUIP",
                                            new AnimationSequence()
                                                    .to(45, 0, 0, 0)
                                                    .to(0, 0, 0, 500, IType.SIN_FULL));
                        case CYCLE:
                            return new BusAnimation()
                                    .addBus(
                                            "RECOIL",
                                            new AnimationSequence()
                                                    .to(
                                                            0,
                                                            0,
                                                            GunBaseNTItem.getIsAiming(stack)
                                                                    ? -0.125
                                                                    : -0.375,
                                                            25,
                                                            IType.SIN_DOWN)
                                                    .to(0, 0, 0, 75, IType.SIN_FULL))
                                    .addBus(
                                            "SAFETY",
                                            new AnimationSequence()
                                                    .to(0.25, 0, 0, 0)
                                                    .to(0.25, 0, 0, 2000)
                                                    .to(0, 0, 0, 50));
                        case CYCLE_DRY:
                            return new BusAnimation()
                                    .addBus(
                                            "BOLT",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 250)
                                                    .to(0, 0, -2, 150)
                                                    .to(0, 0, 0, 100, IType.SIN_UP))
                                    .addBus(
                                            "SAFETY",
                                            new AnimationSequence()
                                                    .to(0.25, 0, 0, 0)
                                                    .to(0.25, 0, 0, 2000)
                                                    .to(0, 0, 0, 50));
                        case RELOAD:
                            return new BusAnimation()
                                    .addBus(
                                            "BOLT",
                                            new AnimationSequence()
                                                    .to(0, 0, -2, 150)
                                                    .to(0, 0, -2, 1600)
                                                    .to(0, 0, 0, 100, IType.SIN_UP))
                                    .addBus(
                                            "HANDLE",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 150)
                                                    .to(0, 0, 20, 50)
                                                    .to(0, 0, 20, 1500)
                                                    .to(0, 0, 0, 50))
                                    .addBus(
                                            "LIFT",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 200)
                                                    .to(-2, 0, 0, 100, IType.SIN_DOWN)
                                                    .to(0, 0, 0, 100, IType.SIN_FULL));
                        case INSPECT:
                            return new BusAnimation()
                                    .addBus(
                                            "BOLT",
                                            new AnimationSequence()
                                                    .to(0, 0, -2, 150)
                                                    .to(0, 0, -2, 6100)
                                                    .to(0, 0, 0, 100, IType.SIN_UP))
                                    .addBus(
                                            "HANDLE",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 150)
                                                    .to(0, 0, 20, 50)
                                                    .to(0, 0, 20, 6000)
                                                    .to(0, 0, 0, 50))
                                    .addBus(
                                            "INSPECT_LEVER",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 500)
                                                    .to(0, 0, -10, 100)
                                                    .to(0, 0, -10, 100)
                                                    .to(0, 0, 0, 100))
                                    .addBus(
                                            "INSPECT_BARREL",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 600)
                                                    .to(0, 0, 20, 150)
                                                    .to(0, 0, 0, 400)
                                                    .to(0, 0, 0, 500)
                                                    .to(15, 0, 0, 500)
                                                    .to(15, 0, 0, 2000)
                                                    .to(0, 0, 0, 500)
                                                    .to(0, 0, 0, 500)
                                                    .to(0, 0, 20, 200)
                                                    .to(0, 0, 20, 400)
                                                    .to(0, 0, 0, 150))
                                    .addBus(
                                            "INSPECT_MOVE",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 750)
                                                    .to(0, 0, 6, 1000)
                                                    .to(2, 0, 3, 500, IType.SIN_FULL)
                                                    .to(2, 0.75, 0, 500, IType.SIN_FULL)
                                                    .to(2, 0.75, 0, 1000)
                                                    .to(2, 0, 3, 500, IType.SIN_FULL)
                                                    .to(0, 0, 6, 500)
                                                    .to(0, 0, 0, 1000))
                                    .addBus(
                                            "INSPECT_GUN",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 1750)
                                                    .to(15, 0, -70, 500, IType.SIN_FULL)
                                                    .to(15, 0, -70, 1500)
                                                    .to(0, 0, 0, 500, IType.SIN_FULL));
                    }
                } else {
                    switch (type) {
                        case EQUIP:
                            return new BusAnimation()
                                    .addBus(
                                            "EQUIP",
                                            new AnimationSequence()
                                                    .to(45, 0, 0, 0)
                                                    .to(0, 0, 0, 500, IType.SIN_FULL));
                        case CYCLE:
                            return new BusAnimation()
                                    .addBus(
                                            "RECOIL",
                                            new AnimationSequence()
                                                    .to(
                                                            0,
                                                            0,
                                                            GunBaseNTItem.getIsAiming(stack)
                                                                    ? -0.125
                                                                    : -0.375,
                                                            25,
                                                            IType.SIN_DOWN)
                                                    .to(0, 0, 0, 75, IType.SIN_FULL))
                                    .addBus(
                                            "SAFETY",
                                            new AnimationSequence()
                                                    .to(0.25, 0, 0, 0)
                                                    .to(0.25, 0, 0, 2000)
                                                    .to(0, 0, 0, 50));
                        case CYCLE_DRY:
                            return ResourceManager.stg77_anim.get("FireDry");
                        case RELOAD:
                            return ResourceManager.stg77_anim.get("Reload");
                        case INSPECT:
                            return ResourceManager.stg77_anim.get("Inspect");
                    }
                }

                return null;
            };

    public static void init() {
        Casing.Template casing556 =
                Casing.template(CasingShape.BOTTLENECK).tints(Casing.BRASS).scale(0.8F);
        r556_sp =
                new BulletConfig()
                        .setItem(EnumAmmo.R556_SP)
                        .setCasing(CasingType.SMALL, 8)
                        .setCasing(casing556.copy().register("r556"));
        r556_fmj =
                new BulletConfig()
                        .setItem(EnumAmmo.R556_FMJ)
                        .setCasing(CasingType.SMALL, 8)
                        .setDamage(0.8F)
                        .setThresholdNegation(4F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing556.copy().register("r556fmj"));
        r556_jhp =
                new BulletConfig()
                        .setItem(EnumAmmo.R556_JHP)
                        .setCasing(CasingType.SMALL, 8)
                        .setDamage(1.5F)
                        .setHeadshot(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing556.copy().register("r556jhp"));
        r556_ap =
                new BulletConfig()
                        .setItem(EnumAmmo.R556_AP)
                        .setCasing(CasingType.SMALL_STEEL, 8)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.25F)
                        .setThresholdNegation(10F)
                        .setArmorPiercing(0.15F)
                        .setCasing(casing556.copy().tints(Casing.MAGNUM_44).register("r556ap"));

        r556_inc_sp = r556_sp.clone().setOnImpact(INCENDIARY);
        r556_inc_fmj = r556_fmj.clone().setOnImpact(INCENDIARY);
        r556_inc_jhp = r556_jhp.clone().setOnImpact(INCENDIARY);
        r556_inc_ap = r556_ap.clone().setOnImpact(INCENDIARY);

        ModItems.GUN_G3 =
                Registrar.registerItem(
                        "gun_g3",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(3_000)
                                                        .draw(10)
                                                        .inspect(33)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(5F)
                                                                        .delay(2)
                                                                        .auto(true)
                                                                        .dry(15)
                                                                        .reload(50)
                                                                        .jam(47)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_ASSAULT_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                r556_sp,
                                                                                                r556_fmj,
                                                                                                r556_jhp,
                                                                                                r556_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_G3))
                                                        .setupStandardConfiguration()
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_SECONDARY)
                                                        .anim(LAMBDA_G3_ANIMS)
                                                        .orchestra(GunAnimationActions.G3))
                                        .setDefaultAmmo(EnumAmmo.R556_SP, 30)
                                        .setNameMutator(LAMBDA_NAME_G3),
                        Item.Properties::new);
        ModItems.GUN_G3_ZEBRA =
                Registrar.registerItem(
                        "gun_g3_zebra",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(6_000)
                                                        .draw(10)
                                                        .inspect(33)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .scopeTexture(scope)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(7.5F)
                                                                        .delay(2)
                                                                        .auto(true)
                                                                        .dry(15)
                                                                        .spreadHipfire(0.01F)
                                                                        .reload(50)
                                                                        .jam(47)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_RIFLE_SILENCER
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                r556_inc_sp,
                                                                                                r556_inc_fmj,
                                                                                                r556_inc_jhp,
                                                                                                r556_inc_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ZEBRA))
                                                        .setupStandardConfiguration()
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_SECONDARY)
                                                        .anim(LAMBDA_G3_ANIMS)
                                                        .orchestra(GunAnimationActions.G3))
                                        .setDefaultAmmo(EnumAmmo.R556_JHP, 30)
                                        .setNameMutator(LAMBDA_NAME_G3),
                        Item.Properties::new);

        ModItems.GUN_STG77 =
                Registrar.registerItem(
                        "gun_stg77",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(3_000)
                                                        .draw(10)
                                                        .inspect(125)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .scopeTexture(scope)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(10F)
                                                                        .delay(2)
                                                                        .dry(15)
                                                                        .auto(true)
                                                                        .reload(46)
                                                                        .jam(0)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_ASSAULT_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                r556_sp,
                                                                                                r556_fmj,
                                                                                                r556_jhp,
                                                                                                r556_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_STG))
                                                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .pt(Lego.LAMBDA_TOGGLE_AIM)
                                                        .decider(LAMBDA_STG77_DECIDER)
                                                        .anim(LAMBDA_STG77_ANIMS)
                                                        .orchestra(GunAnimationActions.STG77))
                                        .setDefaultAmmo(EnumAmmo.R556_FMJ, 30),
                        Item.Properties::new);
    }
}

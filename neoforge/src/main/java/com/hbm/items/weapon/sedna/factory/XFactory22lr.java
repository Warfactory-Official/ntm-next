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

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.config.GunVisualConfig;
import com.hbm.items.ModItems;
import com.hbm.items.variant.CasingType;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class XFactory22lr {

    public static BulletConfig p22_sp;
    public static BulletConfig p22_fmj;
    public static BulletConfig p22_jhp;
    public static BulletConfig p22_ap;
    public static Function<ItemStack, String> LAMBDA_NAME_SILENCED =
            (stack) -> {
                if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER))
                    return stack.getItem().getDescriptionId() + "_silenced";
                return null;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE =
            (stack, ctx) -> {
                Lego.handleStandardSmoke(ctx.entity(), stack, 3000, 0.05D, 1.1D, ctx.configIndex());
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_AM180 =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_STAR_F =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        2.5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_AM180_ANIMS =
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
                                                                    : -0.25,
                                                            15,
                                                            IType.SIN_DOWN)
                                                    .to(0, 0, 0, 35, IType.SIN_FULL));
                        case CYCLE_DRY:
                            return new BusAnimation()
                                    .addBus(
                                            "BOLT",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 550)
                                                    .to(0, 0, -1.5, 100, IType.SIN_UP)
                                                    .to(0, 0, 0, 100, IType.SIN_UP))
                                    .addBus(
                                            "TURN",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 300)
                                                    .to(0, 0, 15, 250, IType.SIN_FULL)
                                                    .to(0, 0, 15, 400)
                                                    .to(0, 0, 0, 250, IType.SIN_FULL));
                        case RELOAD:
                            return new BusAnimation()
                                    .addBus(
                                            "MAGTURN",
                                            new AnimationSequence()
                                                    .to(15, 0, 0, 250, IType.SIN_FULL)
                                                    .to(15, 0, 0, 250)
                                                    .to(15, 0, 70, 300, IType.SIN_FULL)
                                                    .to(15, 0, 0, 0)
                                                    .to(15, 0, 0, 750)
                                                    .to(0, 0, 0, 250, IType.SIN_FULL))
                                    .addBus(
                                            "MAG",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 250)
                                                    .to(2, 0, -4, 250, IType.SIN_FULL)
                                                    .to(-10, 2, -4, 300, IType.SIN_UP)
                                                    .to(3, -6, -4, 0)
                                                    .to(2, 0, -4, 500, IType.SIN_FULL)
                                                    .to(0, 0, 0, 250, IType.SIN_FULL))
                                    .addBus(
                                            "BOLT",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 2250)
                                                    .to(0, 0, -1.5, 100, IType.SIN_UP)
                                                    .to(0, 0, 0, 100, IType.SIN_UP))
                                    .addBus(
                                            "TURN",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 2000)
                                                    .to(0, 0, 15, 250, IType.SIN_FULL)
                                                    .to(0, 0, 15, 400)
                                                    .to(0, 0, 0, 250, IType.SIN_FULL));
                        case JAMMED:
                            return new BusAnimation()
                                    .addBus(
                                            "BOLT",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 750)
                                                    .to(0, 0, -1.5, 100, IType.SIN_UP)
                                                    .to(0, 0, 0, 100, IType.SIN_UP))
                                    .addBus(
                                            "TURN",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 500)
                                                    .to(0, 0, 45, 250, IType.SIN_FULL)
                                                    .to(0, 0, 45, 400)
                                                    .to(0, 0, 0, 250, IType.SIN_FULL));
                        case INSPECT:
                            return new BusAnimation()
                                    .addBus(
                                            "MAGTURN",
                                            new AnimationSequence()
                                                    .to(15, 0, 0, 250, IType.SIN_FULL)
                                                    .to(15, 0, 0, 1400)
                                                    .to(0, 0, 0, 250, IType.SIN_FULL))
                                    .addBus(
                                            "MAG",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 200)
                                                    .to(4, -1, -4, 200, IType.SIN_FULL)
                                                    .to(4, -1.5, -4, 50)
                                                    .to(4, 0, -4, 100)
                                                    .to(4, 6, -4, 250, IType.SIN_DOWN)
                                                    .to(4, 0, -4, 150, IType.SIN_UP)
                                                    .to(4, -1, -4, 100, IType.SIN_DOWN)
                                                    .to(4, -1, -4, 250)
                                                    .to(0, 0, 0, 250, IType.SIN_FULL))
                                    .addBus(
                                            "MAGSPIN",
                                            new AnimationSequence()
                                                    .to(0, 0, 0, 600)
                                                    .to(-400, 0, 0, 500, IType.SIN_FULL)
                                                    .to(-400, 0, 0, 250)
                                                    .to(-360, 0, 0, 250));
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
                            return ResourceManager.am180_anim.get("Fire");
                        case CYCLE_DRY:
                            return ResourceManager.am180_anim.get("FireDry");
                        case RELOAD:
                            return ResourceManager.am180_anim.get("Reload");
                        case JAMMED:
                            return ResourceManager.am180_anim.get("Jammed");
                        case INSPECT:
                            return ResourceManager.am180_anim.get("Inspect");
                    }
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_STAR_F_ANIMS =
            (stack, type) -> {
                int ammo =
                        ((GunBaseNTItem) stack.getItem())
                                .getConfig(stack, 0)
                                .getReceivers(stack)
                                .get(0)
                                .getMagazine(stack)
                                .getAmount(stack, ClientPlayerAccess.player().getInventory());
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(45, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(
                                                        0,
                                                        0,
                                                        GunBaseNTItem.getIsAiming(stack)
                                                                ? -0.125
                                                                : -0.5,
                                                        15,
                                                        IType.SIN_DOWN)
                                                .to(0, 0, 0, 35, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(
                                                        0,
                                                        0,
                                                        GunBaseNTItem.getIsAiming(stack)
                                                                ? -0.5
                                                                : -1,
                                                        25,
                                                        IType.SIN_DOWN)
                                                .to(0, 0, 0, 75, IType.SIN_UP))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(1, 0, 0, 50, IType.SIN_UP)
                                                .to(0, 0, 0, 50, IType.SIN_DOWN))
                                .addBus(
                                        "BULLET",
                                        ammo <= 1
                                                ? new AnimationSequence().jump(100, 0, 0)
                                                : new AnimationSequence()
                                                        .to(0, 0, 0, 90)
                                                        .to(0, 0.5, 2.25, 50));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(1, 0, 0, 50, IType.SIN_UP)
                                                .stay(450)
                                                .to(0, 0, 0, 50, IType.SIN_DOWN))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(
                                                        0,
                                                        0,
                                                        GunBaseNTItem.getIsAiming(stack)
                                                                ? -0.5
                                                                : -1,
                                                        100,
                                                        IType.SIN_FULL)
                                                .stay(100)
                                                .to(0, 0, 0, 75, IType.SIN_UP))
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 600)
                                                .to(-3, 0, 0, 175, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus("BULLET", new AnimationSequence().jump(100, 0, 0));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "TILT",
                                        new AnimationSequence()
                                                .to(-30, 0, 0, 250, IType.SIN_FULL)
                                                .stay(1500)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, -1, 100, IType.SIN_FULL)
                                                .stay(1125)
                                                .to(0, 0, 0, 100, IType.SIN_UP))
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, -7, -1.5, 300, IType.SIN_UP)
                                                .stay(400)
                                                .to(0, 0, 0, 300, IType.SIN_UP))
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(3, 0, 0, 750, IType.SIN_FULL)
                                                .to(-3, 0, 0, 50, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 15, 300, IType.SIN_FULL)
                                                .stay(900)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .jump(ammo <= 1 ? 100 : 0, 0, 0)
                                                .stay(750)
                                                .jump(0, 0, 0)
                                                .stay(750)
                                                .to(0, 0.5, 2.25, 50));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "TILT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(-30, 0, 0, 150, IType.SIN_FULL)
                                                .stay(800)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 25, 150, IType.SIN_FULL)
                                                .stay(800)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 750)
                                                .to(0, 0, -0.5, 100, IType.SIN_FULL)
                                                .stay(100)
                                                .to(0, 0, 0, 100, IType.SIN_UP)
                                                .stay(100)
                                                .to(0, 0, -0.5, 100, IType.SIN_FULL)
                                                .stay(100)
                                                .to(0, 0, 0, 100, IType.SIN_UP))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .jump(0, 0.5, 2.25)
                                                .stay(750)
                                                .to(0, 0.5, 1.25, 100, IType.SIN_FULL)
                                                .stay(100)
                                                .to(0, 0.5, 2.25, 100, IType.SIN_UP)
                                                .stay(100)
                                                .to(0, 0.5, 1.25, 100, IType.SIN_FULL)
                                                .stay(100)
                                                .to(0, 0.5, 2.25, 100, IType.SIN_UP));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "TILT",
                                        new AnimationSequence()
                                                .to(-30, 0, 0, 250, IType.SIN_FULL)
                                                .stay(1500)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 25, 250, IType.SIN_FULL)
                                                .stay(1500)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 350)
                                                .to(0, 0, -0.5, 100, IType.SIN_FULL)
                                                .stay(1125)
                                                .to(0, 0, 0, 100, IType.SIN_UP))
                                .addBus(
                                        "BULLET",
                                        ammo <= 1
                                                ? new AnimationSequence().jump(100, 0, 0)
                                                : new AnimationSequence()
                                                        .jump(0, 0.5, 2.25)
                                                        .stay(350)
                                                        .to(0, 0.5, 1.25, 100, IType.SIN_FULL)
                                                        .stay(1125)
                                                        .to(0, 0.5, 2.25, 100, IType.SIN_UP));
                }

                return null;
            };

    public static void init() {
        Casing.Template casing22 =
                Casing.template(CasingShape.STRAIGHT).tints(Casing.BRASS).scale(0.5F);
        p22_sp =
                new BulletConfig()
                        .setItem(EnumAmmo.P22_SP)
                        .setCasing(CasingType.SMALL, 24)
                        .setKnockback(0F)
                        .setCasing(casing22.copy().register("p22"));
        p22_fmj =
                new BulletConfig()
                        .setItem(EnumAmmo.P22_FMJ)
                        .setCasing(CasingType.SMALL, 24)
                        .setKnockback(0F)
                        .setDamage(0.8F)
                        .setThresholdNegation(1F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing22.copy().register("p22fmj"));
        p22_jhp =
                new BulletConfig()
                        .setItem(EnumAmmo.P22_JHP)
                        .setCasing(CasingType.SMALL, 24)
                        .setKnockback(0F)
                        .setDamage(1.5F)
                        .setHeadshot(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing22.copy().register("p22jhp"));
        p22_ap =
                new BulletConfig()
                        .setItem(EnumAmmo.P22_AP)
                        .setCasing(CasingType.SMALL_STEEL, 24)
                        .setKnockback(0F)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.25F)
                        .setThresholdNegation(2.5F)
                        .setArmorPiercing(0.15F)
                        .setCasing(casing22.copy().tints(Casing.MAGNUM_44).register("p22ap"));

        ModItems.GUN_AM180 =
                Registrar.registerItem(
                        "gun_am180",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(177 * 25)
                                                        .draw(15)
                                                        .inspect(38)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(2F)
                                                                        .delay(1)
                                                                        .dry(10)
                                                                        .auto(true)
                                                                        .spread(0.01F)
                                                                        .reload(66)
                                                                        .jam(30)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_GREASEGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                177)
                                                                                        .addConfigs(
                                                                                                p22_sp,
                                                                                                p22_fmj,
                                                                                                p22_jhp,
                                                                                                p22_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_AM180))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_AM180_ANIMS)
                                                        .orchestra(GunAnimationActions.AM180))
                                        .setDefaultAmmo(EnumAmmo.P22_SP, 35)
                                        .setNameMutator(LAMBDA_NAME_SILENCED),
                        Item.Properties::new);

        ModItems.GUN_STAR_F =
                Registrar.registerItem(
                        "gun_star_f",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(15 * 25)
                                                        .draw(15)
                                                        .inspect(38)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(12.5F)
                                                                        .delay(5)
                                                                        .dry(17)
                                                                        .spread(0.01F)
                                                                        .reload(40)
                                                                        .jam(32)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_STARF_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                15)
                                                                                        .addConfigs(
                                                                                                p22_sp,
                                                                                                p22_fmj,
                                                                                                p22_jhp,
                                                                                                p22_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_STAR_F))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_STAR_F_ANIMS)
                                                        .orchestra(GunAnimationActions.STAR_F))
                                        .setDefaultAmmo(EnumAmmo.P22_SP, 15)
                                        .setNameMutator(LAMBDA_NAME_SILENCED),
                        Item.Properties::new);

        ModItems.GUN_STAR_F_AKIMBO =
                Registrar.registerItem(
                        "gun_star_f_akimbo",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(15 * 25)
                                                        .draw(15)
                                                        .inspect(38)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(12.5F)
                                                                        .delay(5)
                                                                        .dry(17)
                                                                        .spread(0.01F)
                                                                        .reload(40)
                                                                        .jam(32)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_STARF_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                15)
                                                                                        .addConfigs(
                                                                                                p22_sp,
                                                                                                p22_fmj,
                                                                                                p22_jhp,
                                                                                                p22_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_STAR_F))
                                                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(
                                                                GunStateDecider
                                                                        .LAMBDA_STANDARD_DECIDER)
                                                        .anim(LAMBDA_STAR_F_ANIMS)
                                                        .orchestra(
                                                                GunAnimationActions.STAR_F_AKIMBO),
                                                GunConfig.builder()
                                                        .dura(15 * 25)
                                                        .draw(15)
                                                        .inspect(38)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(12.5F)
                                                                        .delay(5)
                                                                        .dry(17)
                                                                        .spread(0.01F)
                                                                        .reload(40)
                                                                        .jam(32)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_STARF_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                1,
                                                                                                15)
                                                                                        .addConfigs(
                                                                                                p22_sp,
                                                                                                p22_fmj,
                                                                                                p22_jhp,
                                                                                                p22_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_STAR_F))
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(
                                                                GunStateDecider
                                                                        .LAMBDA_STANDARD_DECIDER)
                                                        .anim(LAMBDA_STAR_F_ANIMS)
                                                        .orchestra(
                                                                GunAnimationActions.STAR_F_AKIMBO))
                                        .setDefaultAmmo(EnumAmmo.P22_SP, 30),
                        Item.Properties::new);
    }
}

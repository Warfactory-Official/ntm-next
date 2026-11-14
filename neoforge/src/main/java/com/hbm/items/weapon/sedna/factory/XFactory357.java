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

import com.hbm.items.ModItems;
import com.hbm.items.variant.CasingType;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class XFactory357 {

    public static BulletConfig m357_bp;
    public static BulletConfig m357_sp;
    public static BulletConfig m357_fmj;
    public static BulletConfig m357_jhp;
    public static BulletConfig m357_ap;
    public static BulletConfig m357_express;
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ATLAS =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_DANI =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.75));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_ATLAS_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(-90, 0, 0, 0)
                                                .to(0, 0, 0, 350, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(0, 0, -3, 50)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(0, 0, 1, 50)
                                                .to(0, 0, 1, 300)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "DRUM",
                                        new AnimationSequence().to(0, 0, 0, 250).to(0, 0, 1, 200));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(0, 0, 1, 50)
                                                .to(0, 0, 1, 200)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "DRUM",
                                        new AnimationSequence().to(0, 0, 0, 250).to(0, 0, 1, 200));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new AnimationSequence()
                                                .to(0, 0, 90, 300)
                                                .to(0, 0, 90, 2000)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "FRONT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 45, 150)
                                                .to(0, 0, 45, 2000)
                                                .to(0, 0, 0, 75))
                                .addBus(
                                        "RELOAD_ROT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 300)
                                                .to(60, 0, 0, 500)
                                                .to(60, 0, 0, 500)
                                                .to(0, -90, -90, 0)
                                                .to(0, -90, -90, 600)
                                                .to(0, 0, 0, 300)
                                                .to(0, 0, 0, 100)
                                                .to(-45, 0, 0, 50)
                                                .to(-45, 0, 0, 100)
                                                .to(0, 0, 0, 300))
                                .addBus(
                                        "RELOAD_MOVE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 300)
                                                .to(0, -15, 0, 1000)
                                                .to(0, 0, 0, 450))
                                .addBus(
                                        "DRUM_PUSH",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1600)
                                                .to(0, 0, -5, 0)
                                                .to(0, 0, 0, 300));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new AnimationSequence()
                                                .to(0, 0, 90, 300)
                                                .to(0, 0, 90, 1000)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "FRONT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 45, 150)
                                                .to(0, 0, 45, 1000)
                                                .to(0, 0, 0, 75))
                                .addBus(
                                        "RELOAD_ROT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 300)
                                                .to(45, 0, 0, 500, IType.SIN_FULL)
                                                .to(45, 0, 0, 500)
                                                .to(-45, 0, 0, 50)
                                                .to(-45, 0, 0, 100)
                                                .to(0, 0, 0, 300))
                                .addBus(
                                        "RELOAD_MOVE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 300)
                                                .to(0, -2.5, 0, 500, IType.SIN_FULL)
                                                .to(0, -2.5, 0, 500)
                                                .to(0, 0, 0, 350));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LATCH",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 90, 300)
                                                .to(0, 0, 90, 1000)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "FRONT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 45, 150)
                                                .to(0, 0, 45, 1000)
                                                .to(0, 0, 0, 75))
                                .addBus(
                                        "RELOAD_ROT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 0, 300)
                                                .to(45, 0, 0, 500, IType.SIN_FULL)
                                                .to(45, 0, 0, 500)
                                                .to(-45, 0, 0, 50)
                                                .to(-45, 0, 0, 100)
                                                .to(0, 0, 0, 300))
                                .addBus(
                                        "RELOAD_MOVE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 0, 300)
                                                .to(0, -2.5, 0, 500, IType.SIN_FULL)
                                                .to(0, -2.5, 0, 500)
                                                .to(0, 0, 0, 350));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DANI_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(360 * 3, 0, 0, 1000, IType.SIN_DOWN));
                }

                return LAMBDA_ATLAS_ANIMS.apply(stack, type);
            };

    public static void init() {
        m357_bp =
                new BulletConfig()
                        .setItem(EnumAmmo.M357_BP)
                        .setCasing(CasingType.SMALL, 16)
                        .setDamage(0.75F)
                        .setBlackPowder(true);
        m357_sp = new BulletConfig().setItem(EnumAmmo.M357_SP).setCasing(CasingType.SMALL, 8);
        m357_fmj =
                new BulletConfig()
                        .setItem(EnumAmmo.M357_FMJ)
                        .setCasing(CasingType.SMALL, 8)
                        .setDamage(0.8F)
                        .setThresholdNegation(2F)
                        .setArmorPiercing(0.1F);
        m357_jhp =
                new BulletConfig()
                        .setItem(EnumAmmo.M357_JHP)
                        .setCasing(CasingType.SMALL, 8)
                        .setDamage(1.5F)
                        .setHeadshot(1.5F)
                        .setArmorPiercing(-0.25F);
        m357_ap =
                new BulletConfig()
                        .setItem(EnumAmmo.M357_AP)
                        .setCasing(CasingType.SMALL_STEEL, 8)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.25F)
                        .setThresholdNegation(5F)
                        .setArmorPiercing(0.15F);
        m357_express =
                new BulletConfig()
                        .setItem(EnumAmmo.M357_EXPRESS)
                        .setCasing(CasingType.SMALL, 8)
                        .setDoesPenetrate(true)
                        .setDamage(1.5F)
                        .setThresholdNegation(2F)
                        .setArmorPiercing(0.1F)
                        .setWear(1.5F);

        ModItems.GUN_LIGHT_REVOLVER =
                Registrar.registerItem(
                        "gun_light_revolver",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(300)
                                                        .draw(4)
                                                        .inspect(23)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(7.5F)
                                                                        .delay(11)
                                                                        .reload(55)
                                                                        .jam(45)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_PISTOL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                m357_bp,
                                                                                                m357_sp,
                                                                                                m357_fmj,
                                                                                                m357_jhp,
                                                                                                m357_ap,
                                                                                                m357_express))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.3125D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ATLAS))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_ATLAS_ANIMS)
                                                        .orchestra(GunAnimationActions.ATLAS))
                                        .setDefaultAmmo(EnumAmmo.M357_SP, 12),
                        Item.Properties::new);

        ModItems.GUN_LIGHT_REVOLVER_ATLAS =
                Registrar.registerItem(
                        "gun_light_revolver_atlas",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(300)
                                                        .draw(4)
                                                        .inspect(23)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(12.5F)
                                                                        .delay(11)
                                                                        .reload(55)
                                                                        .jam(45)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_PISTOL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                m357_bp,
                                                                                                m357_sp,
                                                                                                m357_fmj,
                                                                                                m357_jhp,
                                                                                                m357_ap,
                                                                                                m357_express))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.3125D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ATLAS))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_ATLAS_ANIMS)
                                                        .orchestra(GunAnimationActions.ATLAS))
                                        .setDefaultAmmo(EnumAmmo.M357_JHP, 12),
                        Item.Properties::new);

        ModItems.GUN_LIGHT_REVOLVER_DANI =
                Registrar.registerItem(
                        "gun_light_revolver_dani",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                GunConfig.builder()
                                                        .dura(30_000)
                                                        .draw(20)
                                                        .inspect(23)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(15F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(11)
                                                                        .reload(55)
                                                                        .jam(45)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_PISTOL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.1F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                m357_bp,
                                                                                                m357_sp,
                                                                                                m357_fmj,
                                                                                                m357_jhp,
                                                                                                m357_ap,
                                                                                                m357_express))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                0.3125D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_DANI))
                                                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(
                                                                GunStateDecider
                                                                        .LAMBDA_STANDARD_DECIDER)
                                                        .anim(LAMBDA_DANI_ANIMS)
                                                        .orchestra(GunAnimationActions.ATLAS),
                                                GunConfig.builder()
                                                        .dura(30_000)
                                                        .draw(20)
                                                        .inspect(23)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(15F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(11)
                                                                        .reload(55)
                                                                        .jam(45)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_PISTOL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                0.9F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                1,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                m357_bp,
                                                                                                m357_sp,
                                                                                                m357_fmj,
                                                                                                m357_jhp,
                                                                                                m357_ap,
                                                                                                m357_express))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.3125D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_DANI))
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(
                                                                GunStateDecider
                                                                        .LAMBDA_STANDARD_DECIDER)
                                                        .anim(LAMBDA_DANI_ANIMS)
                                                        .orchestra(GunAnimationActions.ATLAS))
                                        .setDefaultAmmo(EnumAmmo.M357_EXPRESS, 24),
                        Item.Properties::new);
    }
}

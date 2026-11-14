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

public class XFactoryBlackPowder {

    public static BulletConfig stone =
            new BulletConfig()
                    .setItem(EnumAmmo.STONE)
                    .setBlackPowder(true)
                    .setHeadshot(1F)
                    .setSpread(0.025F)
                    .setRicochetAngle(15);
    public static BulletConfig flint =
            new BulletConfig()
                    .setItem(EnumAmmo.STONE_AP)
                    .setBlackPowder(true)
                    .setHeadshot(1F)
                    .setSpread(0.01F)
                    .setRicochetAngle(5)
                    .setDoesPenetrate(true)
                    .setDamage(1.5F);
    public static BulletConfig iron =
            new BulletConfig()
                    .setItem(EnumAmmo.STONE_IRON)
                    .setBlackPowder(true)
                    .setHeadshot(1F)
                    .setSpread(0F)
                    .setRicochetAngle(90)
                    .setRicochetCount(5)
                    .setDoesPenetrate(true)
                    .setDamageFalloffByPen(false)
                    .setDamage(1.5F);
    public static BulletConfig shot =
            new BulletConfig()
                    .setItem(EnumAmmo.STONE_SHOT)
                    .setBlackPowder(true)
                    .setHeadshot(1F)
                    .setSpread(0.1F)
                    .setRicochetAngle(45)
                    .setProjectiles(6, 6)
                    .setDamage(1F / 6F);
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_PEPPERBOX =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_PEPPERBOX_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence().to(0, 0, 0, 1025).to(60, 0, 0, 250))
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(45, 0, 0, 150, IType.SIN_DOWN)
                                                .to(45, 0, 0, 50)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(80, 0, 0, 25)
                                                .to(80, 0, 0, 1000)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "TRIGGER",
                                        new AnimationSequence()
                                                .to(1, 0, 0, 25)
                                                .to(1, 0, 0, 250)
                                                .to(0, 0, 0, 100));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence().to(0, 0, 0, 525).to(60, 0, 0, 250))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(80, 0, 0, 25)
                                                .to(80, 0, 0, 500)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "TRIGGER",
                                        new AnimationSequence()
                                                .to(1, 0, 0, 25)
                                                .to(1, 0, 0, 250)
                                                .to(0, 0, 0, 100));
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(-45, 0, 0, 0)
                                                .to(0, 0, 0, 200, IType.SIN_DOWN));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(90, 0, 0, 500, IType.SIN_FULL)
                                                .to(90, 0, 0, 1600)
                                                .to(0, 0, 0, 500, IType.SIN_FULL)
                                                .to(-5, 0, 0, 200, IType.SIN_UP)
                                                .to(0, 0, 0, 200, IType.SIN_DOWN))
                                .addBus(
                                        "TRANSLATE",
                                        new AnimationSequence()
                                                .to(0, -12, 5, 500, IType.SIN_FULL)
                                                .to(0, -12, 5, 700)
                                                .to(0, -13, 5, 200)
                                                .to(0, -12, 5, 200)
                                                .to(0, -12, 5, 500)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "LOADER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 5, -5, 0)
                                                .to(0, 0, -0.1, 500, IType.SIN_FULL)
                                                .to(0, 0, -1, 200)
                                                .to(0, 0, -1, 200)
                                                .to(0, 0, -0.1, 200)
                                                .to(0, 5, -5, 500, IType.SIN_FULL)
                                                .to(0, 0, 0, 0))
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 2600)
                                                .to(-360, 0, 0, 750, IType.SIN_FULL))
                                .addBus(
                                        "SHOT",
                                        new AnimationSequence().to(1, 0, 0, 1400).to(0, 0, 0, 0));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence().to(-360, 0, 0, 750, IType.SIN_FULL))
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(-5, 0, 0, 200, IType.SIN_UP)
                                                .to(0, 0, 0, 200, IType.SIN_DOWN));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1300)
                                                .to(60, 0, 0, 500, IType.SIN_FULL)
                                                .to(60, 0, 0, 400)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "TRANSLATE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, -6, 0, 400, IType.SIN_FULL)
                                                .to(0, -6, 0, 2000)
                                                .to(0, 0, 0, 400, IType.SIN_FULL))
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(45, 0, 0, 400, IType.SIN_FULL)
                                                .to(45, 0, 0, 2000)
                                                .to(0, 0, 0, 400, IType.SIN_FULL));
                }

                return null;
            };

    public static void init() {

        ModItems.GUN_PEPPERBOX =
                Registrar.registerItem(
                        "gun_pepperbox",
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
                                                                        .dmg(5F)
                                                                        .delay(27)
                                                                        .reload(67)
                                                                        .jam(58)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_POWDER_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                stone,
                                                                                                flint,
                                                                                                iron,
                                                                                                shot))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_PEPPERBOX))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_PEPPERBOX_ANIMS)
                                                        .orchestra(GunAnimationActions.PEPPERBOX))
                                        .setDefaultAmmo(EnumAmmo.STONE, 12),
                        Item.Properties::new);
    }
}

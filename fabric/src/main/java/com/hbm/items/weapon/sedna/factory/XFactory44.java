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

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.BoxcarEntity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.entity.projectile.TorpedoEntity;
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
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.RegistryUtil;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class XFactory44 {

    public static final Identifier scope_lilmac = RegistryUtil.id("textures/misc/scope_44.png");

    public static BulletConfig m44_bp;
    public static BulletConfig m44_sp;
    public static BulletConfig m44_fmj;
    public static BulletConfig m44_jhp;
    public static BulletConfig m44_ap;
    public static BulletConfig m44_express;
    public static BulletConfig m44_equestrian_pip;
    public static BulletConfig m44_equestrian_mn7;

    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_BOXCAR =
            (bullet, mop) -> {
                BoxcarEntity pippo = new BoxcarEntity(ModEntities.BOXCAR.get(), bullet.level());
                Vec3 hit = mop.getLocation();
                pippo.setPos(hit.x, hit.y + 50, hit.z);
                bullet.level().addFreshEntity(pippo);
                WorldSounds.play(
                        bullet.level(),
                        null,
                        pippo.getX(),
                        pippo.getY() + 50,
                        pippo.getZ(),
                        ModSounds.GUN_GO_GO_GADGET_FUCK_EVERYTHING_IN_THIS_GENERAL_DIRECTION.get(),
                        SoundSource.RECORDS,
                        100F,
                        1F);
                bullet.discard();
            };

    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_TORPEDO =
            (bullet, mop) -> {
                TorpedoEntity murky = new TorpedoEntity(ModEntities.TORPEDO.get(), bullet.level());
                Vec3 hit = mop.getLocation();
                murky.setPos(hit.x, hit.y + 50, hit.z);
                bullet.level().addFreshEntity(murky);
                bullet.discard();
            };
    public static Function<ItemStack, String> LAMBDA_NAME_NOPIP =
            (stack) -> {
                if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE))
                    return stack.getItem().getDescriptionId() + "_scoped";
                return null;
            };
    public static BiConsumer<ItemStack, LambdaContext> SMACK_A_FUCKER =
            (stack, ctx) -> {
                if (GunBaseNTItem.getState(stack, ctx.configIndex()) == GunState.IDLE
                        || GunBaseNTItem.getLastAnim(stack, ctx.configIndex())
                                == GunAnimation.CYCLE) {
                    GunBaseNTItem.setIsAiming(stack, false);
                    GunBaseNTItem.setState(stack, ctx.configIndex(), GunState.DRAWING);
                    GunBaseNTItem.setTimer(
                            stack, ctx.configIndex(), ctx.config().getInspectDuration(stack));
                    GunBaseNTItem.playAnimation(
                            ctx.getPlayer(), stack, GunAnimation.INSPECT, ctx.configIndex());
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_HENRY =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_NOPIP =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_HANGMAN =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_HENRY_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(-90, 0, 0, 0)
                                                .to(0, 0, -3, 350, IType.SIN_DOWN))
                                .addBus(
                                        "SIGHT",
                                        new AnimationSequence()
                                                .to(80, 0, 0, 0)
                                                .to(80, 0, 0, 500)
                                                .to(0, 0, -3, 250, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(0, 0, -1, 50)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "SIGHT",
                                        new AnimationSequence()
                                                .to(35, 0, 0, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 600)
                                                .to(-90, 0, 0, 200)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 600)
                                                .to(0, 0, 45, 200, IType.SIN_DOWN)
                                                .to(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(30, 0, 0, 50)
                                                .to(30, 0, 0, 550)
                                                .to(0, 0, 0, 200));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "LEVER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 600)
                                                .to(-90, 0, 0, 200)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 600)
                                                .to(0, 0, 45, 200, IType.SIN_DOWN)
                                                .to(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(30, 0, 0, 50)
                                                .to(30, 0, 0, 550)
                                                .to(0, 0, 0, 200));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence().to(-60, 0, 0, 400, IType.SIN_FULL))
                                .addBus(
                                        "TWIST",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, -90, 200, IType.SIN_FULL))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 700)
                                                .to(3, 0, -6, 0)
                                                .to(0, 0, 1, 300, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL));
                    case RELOAD_CYCLE:
                        return new BusAnimation()
                                .addBus("LIFT", new AnimationSequence().to(-60, 0, 0, 0))
                                .addBus("TWIST", new AnimationSequence().to(0, 0, -90, 0))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .to(3, 0, -6, 0)
                                                .to(0, 0, 1, 300, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL));
                    case RELOAD_END:
                        boolean empty =
                                ((GunBaseNTItem) stack.getItem())
                                                .getConfig(stack, 0)
                                                .getReceivers(stack)
                                                .get(0)
                                                .getMagazine(stack)
                                                .getAmountBeforeReload(stack)
                                        <= 0;
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(-60, 0, 0, 0)
                                                .to(-60, 0, 0, 300)
                                                .to(0, 0, 0, 400, IType.SIN_FULL))
                                .addBus(
                                        "TWIST",
                                        new AnimationSequence()
                                                .to(0, 0, -90, 0)
                                                .to(0, 0, 0, 200, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 700)
                                                .to(empty ? -90 : 0, 0, 0, 200)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 700)
                                                .to(0, 0, empty ? 45 : 0, 200, IType.SIN_DOWN)
                                                .to(0, 0, 0, 200, IType.SIN_UP));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(-60, 0, 0, 0)
                                                .to(-60, 0, 0, 300)
                                                .to(0, 0, 0, 400, IType.SIN_FULL))
                                .addBus(
                                        "TWIST",
                                        new AnimationSequence()
                                                .to(0, 0, -90, 0)
                                                .to(0, 0, 0, 200, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 700)
                                                .to(-90, 0, 0, 200)
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 0, 500)
                                                .to(-90, 0, 0, 200)
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 0, 200)
                                                .to(-90, 0, 0, 200)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 700)
                                                .to(0, 0, 45, 200, IType.SIN_DOWN)
                                                .to(0, 0, 0, 200, IType.SIN_UP)
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 45, 200, IType.SIN_FULL)
                                                .to(0, 0, 45, 600)
                                                .to(0, 0, 0, 200, IType.SIN_FULL));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "YEET",
                                        new AnimationSequence()
                                                .to(0, 2, 0, 200, IType.SIN_DOWN)
                                                .to(0, 0, 0, 200, IType.SIN_UP))
                                .addBus("ROLL", new AnimationSequence().to(0, 0, 360, 400));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_NOPIP_ANIMS =
            (stack, type) -> {
                switch (type) {
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
                                                .to(0, 0, 1, 400)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "DRUM",
                                        new AnimationSequence().to(0, 0, 0, 450).to(0, 0, 1, 200));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(0, 0, 1, 50)
                                                .to(0, 0, 1, 300 + 100)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "DRUM",
                                        new AnimationSequence().to(0, 0, 0, 450).to(0, 0, 1, 200));
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(90, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "RELAOD_TILT",
                                        new AnimationSequence()
                                                .to(-15, 0, 0, 100)
                                                .to(65, 0, 0, 100)
                                                .to(45, 0, 0, 50)
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 0, 1450)
                                                .to(-80, 0, 0, 100)
                                                .to(-80, 0, 0, 100)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "RELOAD_CYLINDER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(90, 0, 0, 100)
                                                .to(90, 0, 0, 1700)
                                                .to(0, 0, 0, 70))
                                .addBus(
                                        "RELOAD_LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 350)
                                                .to(-45, 0, 0, 250)
                                                .to(-45, 0, 0, 350)
                                                .to(-15, 0, 0, 200)
                                                .to(-15, 0, 0, 1050)
                                                .to(0, 0, 0, 100))
                                .addBus(
                                        "RELOAD_JOLT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 600)
                                                .to(2, 0, 0, 50)
                                                .to(0, 0, 0, 100))
                                .addBus(
                                        "RELOAD_BULLETS",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 650)
                                                .to(10, 0, 0, 300)
                                                .to(10, 0, 0, 200)
                                                .to(0, 0, 0, 700))
                                .addBus(
                                        "RELOAD_BULLETS_CON",
                                        new AnimationSequence()
                                                .to(1, 0, 0, 0)
                                                .to(1, 0, 0, 950)
                                                .to(0, 0, 0, 1));
                    case INSPECT:
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "RELAOD_TILT",
                                        new AnimationSequence()
                                                .to(-15, 0, 0, 100)
                                                .to(65, 0, 0, 100)
                                                .to(45, 0, 0, 50)
                                                .to(0, 0, 0, 200)
                                                .to(0, 0, 0, 200)
                                                .to(-80, 0, 0, 100)
                                                .to(-80, 0, 0, 100)
                                                .to(0, 0, 0, 200))
                                .addBus(
                                        "RELOAD_CYLINDER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(90, 0, 0, 100)
                                                .to(90, 0, 0, 450)
                                                .to(0, 0, 0, 70));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LILMAC_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus("SPIN", new AnimationSequence().to(-360, 0, 0, 350));
                }

                return LAMBDA_NOPIP_ANIMS.apply(stack, type);
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_HANGMAN_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(60, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(0, 0, -3, 50)
                                                .to(0, 0, 0, 250));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LID",
                                        new AnimationSequence()
                                                .to(0, 0, -90, 250)
                                                .to(0, 0, -90, 1500)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, -10, 0, 250, IType.SIN_UP)
                                                .to(0, -10, 0, 500)
                                                .to(0, 0, 0, 350, IType.SIN_FULL))
                                .addBus(
                                        "BULLETS",
                                        new AnimationSequence().to(1, 1, 1, 0).to(0, 0, 0, 500))
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(-15, 0, 0, 500, IType.SIN_FULL)
                                                .to(-15, 0, 0, 850)
                                                .to(-25, 0, 0, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 350, IType.SIN_FULL))
                                .addBus(
                                        "ROLL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 25, 250, IType.SIN_FULL)
                                                .to(0, 0, 25, 1000)
                                                .to(0, 0, 0, 250, IType.SIN_FULL));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 170, 0, 500, IType.SIN_UP)
                                                .to(0, 170, 0, 550)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "ROLL",
                                        new AnimationSequence()
                                                .to(0, 0, 110, 500, IType.SIN_FULL)
                                                .to(0, 0, 110, 550)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "SMACK",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 1, 150, IType.SIN_DOWN)
                                                .to(0, 0, -3, 150, IType.SIN_UP)
                                                .to(0, 0, 0, 350, IType.SIN_FULL));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LID",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, -90, 250)
                                                .to(0, 0, -90, 300)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 0, 250)
                                                .to(0, -3, 0, 150, IType.SIN_UP)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1000)
                                                .to(-10, 0, 0, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 350, IType.SIN_FULL))
                                .addBus(
                                        "ROLL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 25, 250, IType.SIN_FULL)
                                                .to(0, 0, 25, 300)
                                                .to(0, 0, 0, 250, IType.SIN_FULL));
                }

                return null;
            };

    public static void init() {
        Casing.Template casing44 = Casing.template(CasingShape.STRAIGHT).tints(Casing.BRASS);
        m44_bp =
                new BulletConfig()
                        .setItem(EnumAmmo.M44_BP)
                        .setCasing(CasingType.SMALL, 12)
                        .setDamage(0.75F)
                        .setBlackPowder(true)
                        .setCasing(casing44.copy().register("m44bp"));
        m44_sp =
                new BulletConfig()
                        .setItem(EnumAmmo.M44_SP)
                        .setCasing(CasingType.SMALL, 6)
                        .setCasing(casing44.copy().register("m44"));
        m44_fmj =
                new BulletConfig()
                        .setItem(EnumAmmo.M44_FMJ)
                        .setCasing(CasingType.SMALL, 6)
                        .setDamage(0.8F)
                        .setThresholdNegation(3F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing44.copy().register("m44fmj"));
        m44_jhp =
                new BulletConfig()
                        .setItem(EnumAmmo.M44_JHP)
                        .setCasing(CasingType.SMALL, 6)
                        .setDamage(1.5F)
                        .setHeadshot(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing44.copy().register("m44jhp"));
        m44_ap =
                new BulletConfig()
                        .setItem(EnumAmmo.M44_AP)
                        .setCasing(CasingType.SMALL_STEEL, 6)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.25F)
                        .setThresholdNegation(7.5F)
                        .setArmorPiercing(0.15F)
                        .setCasing(casing44.copy().tints(Casing.MAGNUM_44).register("m44ap"));
        m44_express =
                new BulletConfig()
                        .setItem(EnumAmmo.M44_EXPRESS)
                        .setCasing(CasingType.SMALL, 6)
                        .setDoesPenetrate(true)
                        .setDamage(1.5F)
                        .setThresholdNegation(3F)
                        .setArmorPiercing(0.1F)
                        .setWear(1.5F)
                        .setCasing(casing44.copy().register("m44express"));
        m44_equestrian_pip =
                new BulletConfig()
                        .setItem(GunFactory.EnumAmmoSecret.M44_EQUESTRIAN)
                        .setDamage(0F)
                        .setOnImpact(LAMBDA_BOXCAR)
                        .setCasing(
                                casing44.copy()
                                        .tints(Casing.EQUESTRIAN)
                                        .register("m44equestrianPip"));
        m44_equestrian_mn7 =
                new BulletConfig()
                        .setItem(GunFactory.EnumAmmoSecret.M44_EQUESTRIAN)
                        .setDamage(0F)
                        .setOnImpact(LAMBDA_TORPEDO)
                        .setCasing(
                                casing44.copy()
                                        .tints(Casing.EQUESTRIAN)
                                        .register("m44equestrianMn7"));

        ModItems.GUN_HENRY =
                Registrar.registerItem(
                        "gun_henry",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(300)
                                                        .draw(15)
                                                        .inspect(23)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(10F)
                                                                        .delay(20)
                                                                        .reload(25, 11, 14, 8)
                                                                        .jam(45)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_RIFLE_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                14)
                                                                                        .addConfigs(
                                                                                                m44_bp,
                                                                                                m44_sp,
                                                                                                m44_fmj,
                                                                                                m44_jhp,
                                                                                                m44_ap,
                                                                                                m44_express))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_HENRY))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_HENRY_ANIMS)
                                                        .orchestra(GunAnimationActions.HENRY))
                                        .setDefaultAmmo(EnumAmmo.M44_SP, 14),
                        Item.Properties::new);

        ModItems.GUN_HENRY_LINCOLN =
                Registrar.registerItem(
                        "gun_henry_lincoln",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(300)
                                                        .draw(15)
                                                        .inspect(23)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(20F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(20)
                                                                        .reload(25, 11, 14, 8)
                                                                        .jam(45)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_RIFLE_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.25F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                14)
                                                                                        .addConfigs(
                                                                                                m44_bp,
                                                                                                m44_sp,
                                                                                                m44_fmj,
                                                                                                m44_jhp,
                                                                                                m44_ap,
                                                                                                m44_express))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_HENRY))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_HENRY_ANIMS)
                                                        .orchestra(GunAnimationActions.HENRY))
                                        .setDefaultAmmo(EnumAmmo.M44_JHP, 14),
                        Item.Properties::new);

        ModItems.GUN_HEAVY_REVOLVER =
                Registrar.registerItem(
                        "gun_heavy_revolver",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(600)
                                                        .draw(10)
                                                        .inspect(23)
                                                        .crosshair(Crosshair.L_CLASSIC)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(15F)
                                                                        .delay(14)
                                                                        .reload(46)
                                                                        .jam(23)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_HEAVY_REVOLVER_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                m44_bp,
                                                                                                m44_sp,
                                                                                                m44_fmj,
                                                                                                m44_jhp,
                                                                                                m44_ap,
                                                                                                m44_express))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.3125D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_NOPIP))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_NOPIP_ANIMS)
                                                        .orchestra(GunAnimationActions.NOPIP))
                                        .setDefaultAmmo(EnumAmmo.M44_SP, 12)
                                        .setNameMutator(LAMBDA_NAME_NOPIP),
                        Item.Properties::new);

        ModItems.GUN_HEAVY_REVOLVER_LILMAC =
                Registrar.registerItem(
                        "gun_heavy_revolver_lilmac",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                GunConfig.builder()
                                                        .dura(31_000)
                                                        .draw(10)
                                                        .inspect(23)
                                                        .crosshair(Crosshair.L_CLASSIC)
                                                        .scopeTexture(scope_lilmac)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(30F)
                                                                        .delay(14)
                                                                        .reload(46)
                                                                        .jam(23)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_HEAVY_REVOLVER_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                m44_equestrian_pip,
                                                                                                m44_bp,
                                                                                                m44_sp,
                                                                                                m44_fmj,
                                                                                                m44_jhp,
                                                                                                m44_ap,
                                                                                                m44_express))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.3125D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_NOPIP))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_LILMAC_ANIMS)
                                                        .orchestra(GunAnimationActions.NOPIP))
                                        .setDefaultAmmo(EnumAmmo.M44_JHP, 12),
                        Item.Properties::new);

        ModItems.GUN_HEAVY_REVOLVER_PROTEGE =
                Registrar.registerItem(
                        "gun_heavy_revolver_protege",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                GunConfig.builder()
                                                        .dura(31_000)
                                                        .draw(10)
                                                        .inspect(23)
                                                        .crosshair(Crosshair.L_CLASSIC)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(30F)
                                                                        .delay(14)
                                                                        .reload(46)
                                                                        .jam(23)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_HEAVY_REVOLVER_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                0.8F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                6)
                                                                                        .addConfigs(
                                                                                                m44_equestrian_mn7,
                                                                                                m44_bp,
                                                                                                m44_sp,
                                                                                                m44_fmj,
                                                                                                m44_jhp,
                                                                                                m44_ap,
                                                                                                m44_express))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.3125D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_NOPIP))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_LILMAC_ANIMS)
                                                        .orchestra(GunAnimationActions.NOPIP))
                                        .setDefaultAmmo(EnumAmmo.M44_JHP, 12),
                        Item.Properties::new);

        ModItems.GUN_HANGMAN =
                Registrar.registerItem(
                        "gun_hangman",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                GunConfig.builder()
                                                        .dura(600)
                                                        .draw(10)
                                                        .inspect(31)
                                                        .inspectCancel(false)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(25F)
                                                                        .delay(10)
                                                                        .reload(46)
                                                                        .jam(23)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_HEAVY_REVOLVER_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                8)
                                                                                        .addConfigs(
                                                                                                m44_bp,
                                                                                                m44_sp,
                                                                                                m44_fmj,
                                                                                                m44_jhp,
                                                                                                m44_ap,
                                                                                                m44_express))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_HANGMAN))
                                                        .setupStandardConfiguration()
                                                        .ps(SMACK_A_FUCKER)
                                                        .anim(LAMBDA_HANGMAN_ANIMS)
                                                        .orchestra(GunAnimationActions.HANGMAN))
                                        .setDefaultAmmo(EnumAmmo.M44_FMJ, 16),
                        Item.Properties::new);
    }
}

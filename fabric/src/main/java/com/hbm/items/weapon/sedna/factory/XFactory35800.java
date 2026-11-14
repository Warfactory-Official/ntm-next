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

import com.hbm.entity.effect.FireLingeringEntity;
import com.hbm.entity.projectile.BulletBeamBaseEntity;
import com.hbm.extprop.LivingHazards;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class XFactory35800 {

    public static BulletConfig p35800;
    public static BulletConfig p35800_bl;

    public static BiConsumer<BulletBeamBaseEntity, HitResult> LAMBDA_BLACK_IMPACT =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit) {
                    Entity hit = entityHit.getEntity();
                    if (hit instanceof LivingEntity living) {
                        LivingHazards.getOrCreate(living).blackFire += 200;
                    }
                }
                if (mop instanceof BlockHitResult) {
                    Vec3 hit = mop.getLocation();
                    FireLingeringEntity fire =
                            new FireLingeringEntity(bullet.level())
                                    .setArea(7.5F, 2F)
                                    .setDuration(200)
                                    .setType(FireLingeringEntity.TYPE_BLACK);
                    fire.setPos(hit);
                    bullet.level().addFreshEntity(fire);
                }

                BulletConfig.LAMBDA_STANDARD_BEAM_HIT.accept(bullet, mop);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ABERRATOR =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_ABERRATOR =
            (stack, type) -> {
                boolean aim = GunBaseNTItem.getIsAiming(stack);
                int ammo =
                        ((GunBaseNTItem) stack.getItem())
                                .getConfig(stack, 0)
                                .getReceivers(stack)
                                .get(0)
                                .getMagazine(stack)
                                .getAmount(stack, null);
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(360, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "RISE",
                                        new AnimationSequence()
                                                .to(0, -3, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_FULL));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(aim ? -15 : -25, 0, 0, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "SIGHT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(aim ? 5 : 15, 0, 0, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(0, 0, -1.125, 50, IType.SIN_DOWN)
                                                .to(0, 0, -1.125, 50)
                                                .to(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        ammo <= 1 ? "NULL" : "BULLET",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 150)
                                                .to(0, 0.375, 1.125, 150, IType.SIN_UP))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(45, 0, 0, 50)
                                                .to(-45, 0, -1.125, 50, IType.SIN_DOWN)
                                                .to(-20, 0, -1.125, 50)
                                                .to(0, 0, 0, 150, IType.SIN_UP));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 700)
                                                .to(-5, 0, 0, 100, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 550)
                                                .to(0, 0, -1.125, 150, IType.SIN_FULL)
                                                .to(0, 0, -1.125, 50)
                                                .to(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(45, 0, 0, 50)
                                                .to(45, 0, 0, 500)
                                                .to(-45, 0, -1.125, 150, IType.SIN_FULL)
                                                .to(-20, 0, -1.125, 50)
                                                .to(0, 0, 0, 150, IType.SIN_UP));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "ROLL",
                                        new AnimationSequence()
                                                .to(0, 0, 20, 150, IType.SIN_FULL)
                                                .to(0, 0, 20, 50)
                                                .to(0, 0, -45, 150, IType.SIN_UP)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 350)
                                                .to(0, -2, 0, 0)
                                                .to(-15, -5, 0, 350)
                                                .to(-15, 0, 0, 0)
                                                .to(-15, 0, 0, 700)
                                                .to(3, 3, 0, 0)
                                                .to(0, -2, 0, 250, IType.SIN_DOWN)
                                                .to(0, -2, 0, 50)
                                                .to(0, 0, 0, 150, IType.SIN_DOWN))
                                .addBus(
                                        "MAGROLL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 350)
                                                .to(0, 0, -180, 250)
                                                .to(0, 0, 0, 0))
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 750)
                                                .to(5, 0, 0, 150, IType.SIN_FULL)
                                                .to(-190, 0, 0, 500, IType.SIN_FULL)
                                                .to(-190, 0, 0, 450)
                                                .to(-360, 0, 0, 350, IType.SIN_DOWN)
                                                .to(0, 0, 0, 0))
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 2350)
                                                .to(-5, 0, 0, 100, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 2200)
                                                .to(0, 0, -1.125, 150, IType.SIN_FULL)
                                                .to(0, 0, -1.125, 50)
                                                .to(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 2250)
                                                .to(-45, 0, -1.125, 100, IType.SIN_FULL)
                                                .to(-20, 0, -1.125, 50)
                                                .to(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .to(ammo > 0 ? 0 : -100, 0, 0, 0)
                                                .to(ammo > 0 ? 0 : -100, 0, 0, 2400)
                                                .to(0, 0, 0, 0)
                                                .to(0, 0.375, 1.125, 150, IType.SIN_UP));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 0)
                                                .to(-720, 0, 0, 1000, IType.SIN_FULL)
                                                .to(-720, 0, 0, 250)
                                                .to(0, 0, 0, 1000, IType.SIN_FULL));
                    default:
                        return null;
                }
            };

    public static void init() {

        p35800 =
                new BulletConfig()
                        .setItem(EnumAmmoSecret.P35_800)
                        .setArmorPiercing(0.5F)
                        .setThresholdNegation(50F)
                        .setBeam()
                        .setSpread(0.0F)
                        .setLife(3)
                        .setRenderRotations(false)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0xCEB78E)
                                        .register("35-800"))
                        .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
        p35800_bl =
                new BulletConfig()
                        .setItem(EnumAmmoSecret.P35_800_BL)
                        .setArmorPiercing(0.5F)
                        .setThresholdNegation(50F)
                        .setBeam()
                        .setSpread(0.0F)
                        .setLife(3)
                        .setRenderRotations(false)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0xCEB78E)
                                        .register("35-800"))
                        .setOnBeamImpact(LAMBDA_BLACK_IMPACT);

        ModItems.GUN_ABERRATOR =
                Registrar.registerItem(
                        "gun_aberrator",
                        props ->
                                new GunBaseNTItem(
                                        WeaponQuality.SECRET,
                                        props,
                                        GunConfig.builder()
                                                .dura(2_000)
                                                .draw(10)
                                                .inspect(26)
                                                .crosshair(Crosshair.CIRCLE)
                                                .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                .rec(
                                                        Receiver.builder()
                                                                .dmg(100F)
                                                                .delay(13)
                                                                .dry(21)
                                                                .reload(51)
                                                                .sound(
                                                                        () ->
                                                                                ModSounds
                                                                                        .GUN_ABERRATOR_FIRE
                                                                                        .get(),
                                                                        1.0F,
                                                                        1.0F)
                                                                .mag(
                                                                        new MagazineFullReload(0, 5)
                                                                                .addConfigs(
                                                                                        p35800,
                                                                                        p35800_bl))
                                                                .offset(
                                                                        0.75,
                                                                        -0.0625 * 1.5,
                                                                        -0.1875)
                                                                .canFire(
                                                                        Lego
                                                                                .LAMBDA_STANDARD_CAN_FIRE)
                                                                .fire(Lego.LAMBDA_NOWEAR_FIRE)
                                                                .recoil(LAMBDA_RECOIL_ABERRATOR))
                                                .setupStandardConfiguration()
                                                .anim(LAMBDA_ABERRATOR)
                                                .orchestra(GunAnimationActions.ABERRATOR)),
                        Item.Properties::new);

        ModItems.GUN_ABERRATOR_EOTT =
                Registrar.registerItem(
                        "gun_aberrator_eott",
                        props ->
                                new GunBaseNTItem(
                                        WeaponQuality.SECRET,
                                        props,
                                        GunConfig.builder()
                                                .dura(2_000)
                                                .draw(10)
                                                .inspect(26)
                                                .crosshair(Crosshair.CIRCLE)
                                                .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                .rec(
                                                        Receiver.builder()
                                                                .dmg(100F)
                                                                .spreadHipfire(0F)
                                                                .delay(13)
                                                                .dry(21)
                                                                .reload(51)
                                                                .sound(
                                                                        () ->
                                                                                ModSounds
                                                                                        .GUN_ABERRATOR_FIRE
                                                                                        .get(),
                                                                        1.0F,
                                                                        1.0F)
                                                                .mag(
                                                                        new MagazineFullReload(0, 5)
                                                                                .addConfigs(
                                                                                        p35800,
                                                                                        p35800_bl))
                                                                .offset(0.75, -0.0625 * 1.5, 0.1875)
                                                                .canFire(
                                                                        Lego
                                                                                .LAMBDA_STANDARD_CAN_FIRE)
                                                                .fire(Lego.LAMBDA_NOWEAR_FIRE)
                                                                .recoil(LAMBDA_RECOIL_ABERRATOR))
                                                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                                                .anim(LAMBDA_ABERRATOR)
                                                .orchestra(GunAnimationActions.ABERRATOR_EOTT),
                                        GunConfig.builder()
                                                .dura(2_000)
                                                .draw(10)
                                                .inspect(26)
                                                .crosshair(Crosshair.CIRCLE)
                                                .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                .rec(
                                                        Receiver.builder()
                                                                .dmg(100F)
                                                                .spreadHipfire(0F)
                                                                .delay(13)
                                                                .dry(21)
                                                                .reload(51)
                                                                .sound(
                                                                        () ->
                                                                                ModSounds
                                                                                        .GUN_ABERRATOR_FIRE
                                                                                        .get(),
                                                                        1.0F,
                                                                        1.0F)
                                                                .mag(
                                                                        new MagazineFullReload(1, 5)
                                                                                .addConfigs(
                                                                                        p35800,
                                                                                        p35800_bl))
                                                                .offset(
                                                                        0.75,
                                                                        -0.0625 * 1.5,
                                                                        -0.1875)
                                                                .canFire(
                                                                        Lego
                                                                                .LAMBDA_STANDARD_CAN_FIRE)
                                                                .fire(Lego.LAMBDA_NOWEAR_FIRE)
                                                                .recoil(LAMBDA_RECOIL_ABERRATOR))
                                                .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                                                .anim(LAMBDA_ABERRATOR)
                                                .orchestra(GunAnimationActions.ABERRATOR_EOTT)),
                        Item.Properties::new);
    }
}

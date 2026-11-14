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

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.BuildingEntity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.items.ModItems;
import com.hbm.items.variant.CasingType;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class XFactory50 {

    public static final Identifier scope = RegistryUtil.id("textures/misc/scope_amat.png");
    public static final Identifier scope_thermal =
            RegistryUtil.id("textures/misc/scope_penance.png");

    public static BulletConfig bmg50_sp;
    public static BulletConfig bmg50_fmj;
    public static BulletConfig bmg50_jhp;
    public static BulletConfig bmg50_ap;
    public static BulletConfig bmg50_du;
    public static BulletConfig bmg50_he;
    public static BulletConfig bmg50_sm;
    public static BulletConfig bmg50_black;
    public static BulletConfig bmg50_equestrian;

    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_BUILDING =
            (bullet, mop) -> {
                BuildingEntity silver =
                        new BuildingEntity(ModEntities.FALLING_BUILDING.get(), bullet.level());
                Vec3 hit = mop.getLocation();
                silver.setPos(hit.x, hit.y + 50, hit.z);
                bullet.level().addFreshEntity(silver);
                bullet.discard();
            };

    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Lego.tinyExplode(bullet, mop, 2F);
                bullet.discard();
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE =
            (stack, ctx) -> {
                Lego.handleStandardSmoke(ctx.entity(), stack, 2000, 0.05D, 1.1D, 0);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_AMAT =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        12.5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_M2 =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_AMAT_ANIMS =
            (stack, type) -> {
                double turn = -60;
                double pullAmount = -2.5;
                double side = 4;
                double down = -2;
                double detach = 0.5;
                double apex = 7;

                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(45, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "BIPOD",
                                        new AnimationSequence()
                                                .stay(500)
                                                .to(80, 0, 0, 350)
                                                .to(80, 25, 0, 150));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, -0.5, 50, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "BOLT_TURN",
                                        new AnimationSequence()
                                                .stay(250)
                                                .to(0, 0, turn, 150)
                                                .stay(700)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "BOLT_PULL",
                                        new AnimationSequence()
                                                .stay(350)
                                                .to(0, 0, pullAmount, 250, IType.SIN_UP)
                                                .stay(250)
                                                .to(0, 0, 0, 200, IType.LINEAR))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .stay(600)
                                                .to(-3, 0, 0, 150, IType.SIN_DOWN)
                                                .stay(300)
                                                .to(0, 0, 0, 250, IType.SIN_FULL));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "BOLT_TURN",
                                        new AnimationSequence()
                                                .stay(250)
                                                .to(0, 0, turn, 150)
                                                .stay(700)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "BOLT_PULL",
                                        new AnimationSequence()
                                                .stay(350)
                                                .to(0, 0, pullAmount, 250, IType.SIN_UP)
                                                .stay(250)
                                                .to(0, 0, 0, 200, IType.LINEAR))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .stay(600)
                                                .to(-3, 0, 0, 150, IType.SIN_DOWN)
                                                .stay(300)
                                                .to(0, 0, 0, 250, IType.SIN_FULL));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, -10, 0, 350, IType.SIN_UP)
                                                .to(0, 0, 0, 650, IType.SIN_UP))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .stay(1000)
                                                .to(-2, 0, 0, 150, IType.SIN_DOWN)
                                                .to(0, 0, 0, 250, IType.SIN_FULL)
                                                .stay(450)
                                                .to(-3, 0, 0, 150, IType.SIN_DOWN)
                                                .stay(300)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "BOLT_TURN",
                                        new AnimationSequence()
                                                .stay(1500)
                                                .to(0, 0, turn, 150)
                                                .stay(700)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "BOLT_PULL",
                                        new AnimationSequence()
                                                .stay(1600)
                                                .to(0, 0, pullAmount, 250, IType.SIN_UP)
                                                .stay(250)
                                                .to(0, 0, 0, 200, IType.LINEAR));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .stay(250)
                                                .to(-15, 0, 0, 500, IType.SIN_FULL)
                                                .stayUntil(1650)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "BOLT_TURN",
                                        new AnimationSequence()
                                                .stay(250)
                                                .to(0, 0, turn, 150)
                                                .stayUntil(1250)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "BOLT_PULL",
                                        new AnimationSequence()
                                                .stay(350)
                                                .to(0, 0, pullAmount, 250, IType.SIN_UP)
                                                .to(0, 0, 0, 200, IType.LINEAR)
                                                .to(0, 0, pullAmount, 250, IType.SIN_UP)
                                                .to(0, 0, 0, 200, IType.LINEAR));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "SCOPE_THROW",
                                        new AnimationSequence()
                                                .to(0, detach, 0, 100, IType.SIN_FULL)
                                                .to(side, down, 0, 500, IType.SIN_FULL)
                                                .to(side, down - 0.5, 0, 100)
                                                .to(side, apex, 0, 350, IType.SIN_FULL)
                                                .to(side, down - 0.5, 0, 350, IType.SIN_DOWN)
                                                .to(side, down, 0, 100)
                                                .stay(250)
                                                .to(0, detach, 0, 500, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "SCOPE_SPIN",
                                        new AnimationSequence().stay(700).to(-360, 0, 0, 700));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_M2_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(80, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_FULL));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, -0.25, 25)
                                                .to(0, 0, 0, 75));
                }

                return null;
            };

    public static void init() {
        Casing.Template casing50 =
                Casing.template(CasingShape.BOTTLENECK).tints(Casing.BRASS).scale(1.5F);
        bmg50_sp =
                new BulletConfig()
                        .setItem(EnumAmmo.BMG50_SP)
                        .setCasing(CasingType.LARGE, 12)
                        .setCasing(casing50.copy().register("bmg50"));
        bmg50_fmj =
                new BulletConfig()
                        .setItem(EnumAmmo.BMG50_FMJ)
                        .setCasing(CasingType.LARGE, 12)
                        .setDamage(0.8F)
                        .setThresholdNegation(7F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing50.copy().register("bmg50fmj"));
        bmg50_jhp =
                new BulletConfig()
                        .setItem(EnumAmmo.BMG50_JHP)
                        .setCasing(CasingType.LARGE, 12)
                        .setDamage(1.5F)
                        .setHeadshot(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing50.copy().register("bmg50jhp"));
        bmg50_ap =
                new BulletConfig()
                        .setItem(EnumAmmo.BMG50_AP)
                        .setCasing(CasingType.LARGE_STEEL, 12)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.25F)
                        .setThresholdNegation(17.5F)
                        .setArmorPiercing(0.15F)
                        .setCasing(casing50.copy().tints(Casing.MAGNUM_44).register("bmg50ap"));
        bmg50_du =
                new BulletConfig()
                        .setItem(EnumAmmo.BMG50_DU)
                        .setCasing(CasingType.LARGE_STEEL, 12)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.5F)
                        .setThresholdNegation(21F)
                        .setArmorPiercing(0.25F)
                        .setCasing(casing50.copy().tints(Casing.MAGNUM_44).register("bmg50du"));
        bmg50_he =
                new BulletConfig()
                        .setItem(EnumAmmo.BMG50_HE)
                        .setCasing(CasingType.LARGE_STEEL, 12)
                        .setWear(3F)
                        .setDamage(1.75F)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE)
                        .setCasing(casing50.copy().tints(Casing.MAGNUM_44).register("bmg50he"));
        bmg50_sm =
                new BulletConfig()
                        .setItem(EnumAmmo.BMG50_SM)
                        .setCasing(CasingType.LARGE_STEEL, 6)
                        .setWear(10F)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(2.5F)
                        .setThresholdNegation(30F)
                        .setArmorPiercing(0.35F)
                        .setCasing(casing50.copy().tints(Casing.MAGNUM_44).register("bmg50sm"));
        bmg50_black =
                new BulletConfig()
                        .setItem(GunFactory.EnumAmmoSecret.BMG50_BLACK)
                        .setWear(5F)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setSpectral(true)
                        .setDamage(1.5F)
                        .setHeadshot(3F)
                        .setThresholdNegation(30F)
                        .setArmorPiercing(0.35F)
                        .setCasing(casing50.copy().tints(Casing.EQUESTRIAN).register("bmg50black"));
        bmg50_equestrian =
                new BulletConfig()
                        .setItem(GunFactory.EnumAmmoSecret.BMG50_EQUESTRIAN)
                        .setDamage(0F)
                        .setOnImpact(LAMBDA_BUILDING)
                        .setCasing(
                                casing50.copy()
                                        .tints(Casing.EQUESTRIAN)
                                        .register("bmg50equestrian"));

        ModItems.GUN_AMAT =
                Registrar.registerItem(
                        "gun_amat",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(350)
                                                        .draw(20)
                                                        .inspect(50)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .scopeTexture(scope)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(30F)
                                                                        .delay(25)
                                                                        .dry(25)
                                                                        .spreadHipfire(0.05F)
                                                                        .reload(51)
                                                                        .jam(43)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_AMAT_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                7)
                                                                                        .addConfigs(
                                                                                                bmg50_sp,
                                                                                                bmg50_fmj,
                                                                                                bmg50_jhp,
                                                                                                bmg50_ap,
                                                                                                bmg50_du,
                                                                                                bmg50_sm,
                                                                                                bmg50_he))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_AMAT))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_AMAT_ANIMS)
                                                        .orchestra(GunAnimationActions.AMAT))
                                        .setDefaultAmmo(EnumAmmo.BMG50_SP, 7),
                        Item.Properties::new);
        ModItems.GUN_AMAT_SUBTLETY =
                Registrar.registerItem(
                        "gun_amat_subtlety",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                GunConfig.builder()
                                                        .dura(1_000)
                                                        .draw(20)
                                                        .inspect(50)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .scopeTexture(scope)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(50F)
                                                                        .delay(25)
                                                                        .dry(25)
                                                                        .spreadHipfire(0.05F)
                                                                        .reload(51)
                                                                        .jam(43)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_AMAT_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                7)
                                                                                        .addConfigs(
                                                                                                bmg50_equestrian,
                                                                                                bmg50_sp,
                                                                                                bmg50_fmj,
                                                                                                bmg50_jhp,
                                                                                                bmg50_ap,
                                                                                                bmg50_du,
                                                                                                bmg50_sm,
                                                                                                bmg50_he))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_AMAT))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_AMAT_ANIMS)
                                                        .orchestra(GunAnimationActions.AMAT))
                                        .setDefaultAmmo(EnumAmmo.BMG50_JHP, 7),
                        Item.Properties::new);
        ModItems.GUN_AMAT_PENANCE =
                Registrar.registerItem(
                        "gun_amat_penance",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                GunConfig.builder()
                                                        .dura(5_000)
                                                        .draw(20)
                                                        .inspect(50)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .scopeTexture(scope_thermal)
                                                        .thermalSights(true)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(45F)
                                                                        .delay(25)
                                                                        .dry(25)
                                                                        .spreadHipfire(0F)
                                                                        .reload(51)
                                                                        .jam(43)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_AMAT_SILENCER
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                7)
                                                                                        .addConfigs(
                                                                                                bmg50_sp,
                                                                                                bmg50_fmj,
                                                                                                bmg50_jhp,
                                                                                                bmg50_ap,
                                                                                                bmg50_du,
                                                                                                bmg50_sm,
                                                                                                bmg50_he,
                                                                                                bmg50_black))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_AMAT))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_AMAT_ANIMS)
                                                        .orchestra(GunAnimationActions.AMAT))
                                        .setDefaultAmmo(EnumAmmo.BMG50_JHP, 7),
                        Item.Properties::new);

        ModItems.GUN_M2 =
                Registrar.registerItem(
                        "gun_m2",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(3_000)
                                                        .draw(10)
                                                        .inspect(31)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(7.5F)
                                                                        .delay(2)
                                                                        .dry(10)
                                                                        .auto(true)
                                                                        .spread(0.005F)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .TURRET_50BMG
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineBelt()
                                                                                        .addConfigs(
                                                                                                bmg50_sp,
                                                                                                bmg50_fmj,
                                                                                                bmg50_jhp,
                                                                                                bmg50_ap,
                                                                                                bmg50_du,
                                                                                                bmg50_he))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_M2))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_M2_ANIMS)
                                                        .orchestra(GunAnimationActions.M2))
                                        .setDefaultAmmo(EnumAmmo.BMG50_FMJ, 25),
                        Item.Properties::new);
    }
}

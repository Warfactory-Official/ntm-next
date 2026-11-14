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

import com.hbm.entity.projectile.BulletBaseMK4Entity;
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
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class XFactory10ga {

    public static BulletConfig g10;
    public static BulletConfig g10_shrapnel;
    public static BulletConfig g10_du;
    public static BulletConfig g10_slug;
    public static BulletConfig g10_explosive;

    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_TINY_EXPLODE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Lego.tinyExplode(bullet, mop, 1.5F);
                bullet.discard();
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_DOUBLE_BARREL =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_DOUBLE_SECONDARY =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                Player player = ctx.getPlayer();
                Receiver rec = ctx.config().getReceivers(stack).get(0);
                int index = ctx.configIndex();
                GunState state = GunBaseNTItem.getState(stack, index);

                if (state == GunState.IDLE) {

                    if (rec.getCanFire(stack).apply(stack, ctx)) {
                        rec.getOnFire(stack).accept(stack, ctx);

                        if (rec.getFireSound(stack) != null)
                            EntitySounds.play(
                                    entity,
                                    entity.getX(),
                                    entity.getY(),
                                    entity.getZ(),
                                    rec.getFireSound(stack).get(),
                                    SoundSource.PLAYERS,
                                    rec.getFireVolume(stack),
                                    rec.getFirePitch(stack));

                        GunBaseNTItem.setState(stack, index, GunState.COOLDOWN);
                        GunBaseNTItem.setTimer(stack, index, rec.getDelayAfterFire(stack));
                    } else {

                        if (rec.getDoesDryFire(stack)) {
                            GunBaseNTItem.playAnimation(
                                    player, stack, GunAnimation.CYCLE_DRY, index);
                            GunBaseNTItem.setState(
                                    stack,
                                    index,
                                    rec.getRefireAfterDry(stack)
                                            ? GunState.COOLDOWN
                                            : GunState.DRAWING);
                            GunBaseNTItem.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
                        }
                    }
                }

                if (state == GunState.RELOADING) {
                    GunBaseNTItem.setReloadCancel(stack, true);
                }
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DOUBLE_BARREL_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(-60, 0, 0, 0)
                                                .to(0, 0, -3, 500, IType.SIN_DOWN));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence().to(0, 0, -1, 50).to(0, 0, 0, 250))
                                .addBus(
                                        "BUCKLE",
                                        new AnimationSequence().to(0, -60, 0, 50).to(0, 0, 0, 250));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 30, 0, 350, IType.SIN_FULL)
                                                .to(0, 30, 0, 1150)
                                                .to(0, 0, 0, 350, IType.SIN_FULL))
                                .addBus(
                                        "LEVER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, -90, 100, IType.SIN_FULL)
                                                .to(0, 0, -90, 1300)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "BARREL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 300)
                                                .to(60, 0, 0, 150, IType.SIN_UP)
                                                .to(60, 0, 0, 1150)
                                                .to(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 350)
                                                .to(-5, 0, 0, 150, IType.SIN_FULL)
                                                .to(0, 0, 0, 100, IType.SIN_FULL)
                                                .to(0, 0, 0, 700)
                                                .to(-5, 0, 0, 100, IType.SIN_FULL)
                                                .to(0, 0, 0, 100, IType.SIN_UP)
                                                .to(45, 0, 0, 150)
                                                .to(45, 0, 0, 150)
                                                .to(-5, 0, 0, 150, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "SHELLS",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 450)
                                                .to(0, 0, -2.5, 100)
                                                .to(0, -5, -5, 350, IType.SIN_DOWN)
                                                .to(0, -3, -2, 0)
                                                .to(0, 0, -2, 250)
                                                .to(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "SHELL_FLIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 450)
                                                .to(-360, 0, 0, 450)
                                                .to(0, 0, 0, 0));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "LEVER",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, -90, 100, IType.SIN_FULL)
                                                .to(0, 0, -90, 800)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "BARREL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 300)
                                                .to(60, 0, 0, 150, IType.SIN_UP)
                                                .to(60, 0, 0, 650)
                                                .to(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 350)
                                                .to(-5, 0, 0, 150, IType.SIN_FULL)
                                                .to(0, 0, 0, 100, IType.SIN_FULL)
                                                .to(0, 0, 0, 200)
                                                .to(-5, 0, 0, 100, IType.SIN_FULL)
                                                .to(0, 0, 0, 100, IType.SIN_UP)
                                                .to(45, 0, 0, 150)
                                                .to(45, 0, 0, 150)
                                                .to(-5, 0, 0, 150, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL));
                }

                return null;
            };

    public static void init() {

        float buckshotSpread = 0.035F;
        g10 =
                new BulletConfig()
                        .setItem(EnumAmmo.G10)
                        .setCasing(CasingType.BUCKSHOT_ADVANCED, 4)
                        .setProjectiles(10)
                        .setDamage(1F / 10F)
                        .setSpread(buckshotSpread)
                        .setRicochetAngle(15)
                        .setThresholdNegation(5F)
                        .setCasing(
                                Casing.template(CasingShape.SHOTGUN)
                                        .tints(0xB52B2B, Casing.GAUGE_12)
                                        .scale(1F)
                                        .register("10GA"));
        g10_shrapnel =
                new BulletConfig()
                        .setItem(EnumAmmo.G10_SHRAPNEL)
                        .setCasing(CasingType.BUCKSHOT_ADVANCED, 4)
                        .setProjectiles(10)
                        .setDamage(1F / 10F)
                        .setSpread(buckshotSpread)
                        .setRicochetAngle(90)
                        .setRicochetCount(15)
                        .setThresholdNegation(5F)
                        .setCasing(
                                Casing.template(CasingShape.SHOTGUN)
                                        .tints(0xE5DD00, Casing.GAUGE_12)
                                        .scale(1F)
                                        .register("10GAShrapnel"));
        g10_du =
                new BulletConfig()
                        .setItem(EnumAmmo.G10_DU)
                        .setCasing(CasingType.BUCKSHOT_ADVANCED, 4)
                        .setProjectiles(10)
                        .setDamage(1F / 4F)
                        .setSpread(buckshotSpread)
                        .setRicochetAngle(15)
                        .setThresholdNegation(10F)
                        .setArmorPiercing(0.2F)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setCasing(
                                Casing.template(CasingShape.SHOTGUN)
                                        .tints(0x538D53, Casing.GAUGE_12)
                                        .scale(1F)
                                        .register("10GADU"));
        g10_slug =
                new BulletConfig()
                        .setItem(EnumAmmo.G10_SLUG)
                        .setCasing(CasingType.BUCKSHOT_ADVANCED, 4)
                        .setRicochetAngle(15)
                        .setThresholdNegation(10F)
                        .setArmorPiercing(0.1F)
                        .setDoesPenetrate(true)
                        .setCasing(
                                Casing.template(CasingShape.SHOTGUN)
                                        .tints(0x808080, Casing.GAUGE_12)
                                        .scale(1F)
                                        .register("10GASlug"));
        g10_explosive =
                new BulletConfig()
                        .setItem(EnumAmmo.G10_EXPLOSIVE)
                        .setCasing(CasingType.BUCKSHOT_ADVANCED, 4)
                        .setWear(3F)
                        .setProjectiles(10)
                        .setDamage(1F / 4F)
                        .setSpread(buckshotSpread)
                        .setCasing(
                                Casing.template(CasingShape.SHOTGUN)
                                        .tints(0xFAC943, Casing.GAUGE_12)
                                        .scale(1F)
                                        .register("10GAEXP"))
                        .setOnImpact(LAMBDA_TINY_EXPLODE);

        ModItems.GUN_DOUBLE_BARREL =
                Registrar.registerItem(
                        "gun_double_barrel",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.SPECIAL,
                                                props,
                                                GunConfig.builder()
                                                        .dura(1000)
                                                        .draw(10)
                                                        .inspect(39)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(30F)
                                                                        .rounds(2)
                                                                        .delay(10)
                                                                        .reload(41)
                                                                        .reloadOnEmpty(true)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHOTGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                0.9F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                2)
                                                                                        .addConfigs(
                                                                                                g10,
                                                                                                g10_shrapnel,
                                                                                                g10_du,
                                                                                                g10_slug,
                                                                                                g10_explosive))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_DOUBLE_BARREL))
                                                        .setupStandardConfiguration()
                                                        .ps(LAMBDA_DOUBLE_SECONDARY)
                                                        .anim(LAMBDA_DOUBLE_BARREL_ANIMS)
                                                        .orchestra(
                                                                GunAnimationActions.DOUBLE_BARREL))
                                        .setDefaultAmmo(EnumAmmo.G10, 6),
                        Item.Properties::new);

        ModItems.GUN_DOUBLE_BARREL_SACRED_DRAGON =
                Registrar.registerItem(
                        "gun_double_barrel_sacred_dragon",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(6000)
                                                        .draw(10)
                                                        .inspect(39)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(45F)
                                                                        .spreadAmmo(1.35F)
                                                                        .rounds(2)
                                                                        .delay(10)
                                                                        .reload(41)
                                                                        .reloadOnEmpty(true)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHOTGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                0.9F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                2)
                                                                                        .addConfigs(
                                                                                                g10,
                                                                                                g10_shrapnel,
                                                                                                g10_du,
                                                                                                g10_slug,
                                                                                                g10_explosive))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_DOUBLE_BARREL))
                                                        .setupStandardConfiguration()
                                                        .ps(LAMBDA_DOUBLE_SECONDARY)
                                                        .anim(LAMBDA_DOUBLE_BARREL_ANIMS)
                                                        .orchestra(
                                                                GunAnimationActions.DOUBLE_BARREL))
                                        .setDefaultAmmo(EnumAmmo.G10_DU, 6),
                        Item.Properties::new);

        ModItems.GUN_AUTOSHOTGUN_HERETIC =
                Registrar.registerItem(
                        "gun_autoshotgun_heretic",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.DEBUG,
                                                props,
                                                GunConfig.builder()
                                                        .draw(20)
                                                        .inspect(65)
                                                        .reloadSequential(true)
                                                        .inspectCancel(false)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .hideCrosshair(false)
                                                        .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(100F)
                                                                        .delay(3)
                                                                        .auto(true)
                                                                        .dryfireAfterAuto(true)
                                                                        .reload(110)
                                                                        .jam(19)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_SHREDDER_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                250)
                                                                                        .addConfigs(
                                                                                                g10,
                                                                                                g10_shrapnel,
                                                                                                g10_du,
                                                                                                g10_slug,
                                                                                                g10_explosive))
                                                                        .offset(0.75, -0.125, -0.25)
                                                                        .canFire(
                                                                                Lego
                                                                                        .LAMBDA_STANDARD_CAN_FIRE)
                                                                        .fire(
                                                                                Lego
                                                                                        .LAMBDA_NOWEAR_FIRE)
                                                                        .recoil(
                                                                                XFactory12ga
                                                                                        .LAMBDA_RECOIL_SEXY))
                                                        .setupStandardConfiguration()
                                                        .anim(XFactory12ga.LAMBDA_SEXY_ANIMS)
                                                        .orchestra(
                                                                GunAnimationActions.SHREDDER_SEXY))
                                        .setDefaultAmmo(EnumAmmo.G10, 50),
                        Item.Properties::new);
    }
}

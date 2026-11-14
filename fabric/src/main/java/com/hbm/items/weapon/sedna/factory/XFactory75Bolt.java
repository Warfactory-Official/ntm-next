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
import com.hbm.extprop.LivingHazards;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
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
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class XFactory75Bolt {

    public static BulletConfig b75;
    public static BulletConfig b75_inc;
    public static BulletConfig b75_exp;

    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_TINY_EXPLODE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Lego.tinyExplode(bullet, mop, 2F);
                bullet.discard();
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_INC =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    LivingHazards data = LivingHazards.getOrCreate(living);
                    if (data.phosphorus < 300) data.phosphorus = 300;
                }
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE =
            (bullet, mop) -> {
                Lego.standardExplode(bullet, mop, 5F);
                bullet.discard();
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE =
            (stack, ctx) -> {
                Lego.handleStandardSmoke(ctx.entity(), stack, 2000, 0.05D, 1.1D, 0);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_BOLT =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_BOLTER_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence().to(1, 0, 0, 25).to(0, 0, 0, 75));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "TILT",
                                        new AnimationSequence()
                                                .to(1, 0, 0, 250)
                                                .to(1, 0, 0, 1500)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, 0, 1, 500)
                                                .to(1, 0, 1, 500)
                                                .to(0, 0, 0, 500));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "TILT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(1, 0, 0, 250)
                                                .to(1, 0, 0, 700)
                                                .to(0, 0, 0, 250))
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 750)
                                                .to(0.6, 0, 0, 250)
                                                .to(0, 0, 0, 250));
                }

                return null;
            };

    public static void init() {
        Casing.Template casing75 =
                Casing.template(CasingShape.STRAIGHT).tints(Casing.BRASS).scale(2F, 2F, 1.5F);

        b75 =
                new BulletConfig()
                        .setItem(EnumAmmo.B75)
                        .setCasing(casing75.copy().register("b75"))
                        .setOnImpact(LAMBDA_TINY_EXPLODE);
        b75_inc =
                new BulletConfig()
                        .setItem(EnumAmmo.B75_INC)
                        .setDamage(0.8F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing75.copy().register("b75inc"))
                        .setOnImpact(LAMBDA_INC);
        b75_exp =
                new BulletConfig()
                        .setItem(EnumAmmo.B75_EXP)
                        .setDamage(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing75.copy().register("b75exp"))
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE);

        ModItems.GUN_BOLTER =
                Registrar.registerItem(
                        "gun_bolter",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.SPECIAL,
                                                props,
                                                GunConfig.builder()
                                                        .dura(3_000)
                                                        .draw(20)
                                                        .inspect(31)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(15F)
                                                                        .delay(2)
                                                                        .auto(true)
                                                                        .spread(0.005F)
                                                                        .reload(40)
                                                                        .jam(55)
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
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                b75,
                                                                                                b75_inc,
                                                                                                b75_exp))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_BOLT))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_BOLTER_ANIMS)
                                                        .orchestra(GunAnimationActions.BOLTER))
                                        .setDefaultAmmo(EnumAmmo.B75, 15),
                        Item.Properties::new);
    }
}

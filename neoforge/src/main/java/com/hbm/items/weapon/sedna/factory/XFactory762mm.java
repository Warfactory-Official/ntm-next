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
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.variant.CasingType;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.ModDamageTypes;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class XFactory762mm {

    public static BulletConfig r762_sp;
    public static BulletConfig r762_fmj;
    public static BulletConfig r762_jhp;
    public static BulletConfig r762_ap;
    public static BulletConfig r762_du;
    public static BulletConfig r762_he;

    public static BulletConfig energy_lacunae;
    public static BulletConfig energy_lacunae_overcharge;
    public static BulletConfig energy_lacunae_ir;

    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_TINY_EXPLODE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Lego.tinyExplode(bullet, mop, 1.5F);
                bullet.discard();
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_MINIGUN =
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
                            return GunBaseNTItem.getSecondary(stack, index)
                                    && GunBaseNTItem.getMode(stack, ctx.configIndex()) == 0;
                        });
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE =
            (stack, ctx) -> {
                Lego.handleStandardSmoke(ctx.entity(), stack, 1500, 0.075D, 1.1D, 0);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_CARBINE =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MINIGUN =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_LACUNAE = (stack, ctx) -> {};

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CARBINE_ANIMS =
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
                                                                ? -0.25
                                                                : -0.5,
                                                        50,
                                                        IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, -1, 50, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_UP))
                                .addBus(
                                        ammo <= 1 ? "NULL" : "REL",
                                        new AnimationSequence()
                                                .to(0, 0, 0.25, 50)
                                                .to(0, 0.125, 1.25, 100, IType.SIN_UP));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, -1, 100, IType.SIN_DOWN)
                                                .to(0, 0, -1, 50)
                                                .to(0, 0, 0, 100, IType.SIN_UP));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, -4, 0, 250, IType.SIN_UP)
                                                .to(0, -4, 0, 750)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 1000))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .to(ammo == 0 ? 1 : 0, 0, 0, 0)
                                                .to(0, 0, 0, 1000));
                    case RELOAD_END:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(-25, 0, 0, 0)
                                                .to(-25, 0, 0, 750)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, -1, 100, IType.SIN_DOWN)
                                                .to(0, 0, -1, 50)
                                                .to(0, 0, 0, 100, IType.SIN_UP))
                                .addBus(
                                        "REL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, 0.25, 150)
                                                .to(0, 0.125, 1.25, 100, IType.SIN_UP));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(-25, 0, 0, 0)
                                                .to(-25, 0, 0, 750)
                                                .to(0, 0, 0, 500, IType.SIN_FULL)
                                                .to(0, 0, 0, 250)
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 750)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, -1, 100, IType.SIN_DOWN)
                                                .to(0, 0, -1, 50)
                                                .to(0, 0, -0.25, 100, IType.SIN_UP)
                                                .to(0, 0, -0.25, 1250)
                                                .to(0, 0, -1, 100, IType.SIN_DOWN)
                                                .to(0, 0, -1, 50)
                                                .to(0, 0, 0, 100, IType.SIN_UP))
                                .addBus(
                                        "REL",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, 0.25, 150)
                                                .to(0, 0.125, 1, 100, IType.SIN_UP)
                                                .to(0, 0.125, 1, 1250)
                                                .to(0, 0.125, 0.25, 100, IType.SIN_DOWN)
                                                .to(0, 0.125, 1, 100, IType.SIN_UP));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 1500)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, -0.75, 150, IType.SIN_DOWN)
                                                .to(0, 0, -0.75, 1000)
                                                .to(0, 0, 0, 100, IType.SIN_UP))
                                .addBus(
                                        ammo == 0 ? "NULL" : "REL",
                                        new AnimationSequence()
                                                .to(0, 0.125, 1.25, 0)
                                                .to(0, 0.125, 1.25, 500)
                                                .to(0, 0.125, 0.5, 150, IType.SIN_DOWN)
                                                .to(0, 0.125, 0.5, 1000)
                                                .to(0, 0.125, 1.25, 100, IType.SIN_UP));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MINIGUN_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(45, 0, 0, 0)
                                                .to(0, 0, 0, 1000, IType.SIN_FULL));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(
                                                        0,
                                                        0,
                                                        GunBaseNTItem.getIsAiming(stack)
                                                                ? -0.25
                                                                : -0.5,
                                                        0)
                                                .to(
                                                        0,
                                                        0,
                                                        GunBaseNTItem.getIsAiming(stack)
                                                                ? -0.25
                                                                : -0.5,
                                                        100)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, 60, 50)
                                                .to(0, 0, 720, 1000, IType.SIN_DOWN));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, 60, 50)
                                                .to(0, 0, 720, 1000, IType.SIN_DOWN));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(-15, 0, 0, 250, IType.SIN_DOWN)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, 60, 50)
                                                .to(0, 0, 720, 1000, IType.SIN_DOWN));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(3, 0, 0, 150, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, -720, 1000, IType.SIN_DOWN));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MAS36_ANIMS =
            (stack, type) -> {
                int mag =
                        ((GunBaseNTItem) stack.getItem())
                                .getConfig(stack, 0)
                                .getReceivers(stack)
                                .get(0)
                                .getMagazine(stack)
                                .getAmount(stack, ClientPlayerAccess.player().getInventory());
                double turn = -90;
                double pullAmount = GunBaseNTItem.getIsAiming(stack) ? -1F : -1.5D;
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "STOCK",
                                        new AnimationSequence()
                                                .jump(-158, 0, 0)
                                                .stay(500)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .jump(45, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_FULL)
                                                .stay(500)
                                                .to(1, 0, 0, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL));
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
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "BULLET",
                                        mag <= 1
                                                ? new AnimationSequence().jump(-100, 0, 0)
                                                : new AnimationSequence()
                                                        .stay(850)
                                                        .to(0, 0.1875, 1.5, 200, IType.LINEAR));
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
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus("BULLET", new AnimationSequence().jump(-100, 0, 0));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "BOLT_TURN",
                                        new AnimationSequence()
                                                .to(0, 0, turn, 150)
                                                .stayUntil(2000)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "BOLT_PULL",
                                        new AnimationSequence()
                                                .stay(100)
                                                .to(0, 0, -1.5D, 250, IType.SIN_UP)
                                                .stayUntil(1800)
                                                .to(0, 0, 0, 200, IType.LINEAR))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .jump(-100, 0, 0)
                                                .stayUntil(1200)
                                                .jump(0, 0, 0)
                                                .stay(600)
                                                .to(0, 0.1875, 1.5, 200, IType.LINEAR))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .stay(200)
                                                .to(30, 0, 0, 500, IType.SIN_FULL)
                                                .stayUntil(1200)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus("SHOW_CLIP", new AnimationSequence().jump(1, 1, 1))
                                .addBus(
                                        "CLIP",
                                        new AnimationSequence()
                                                .jump(2, -3, 0)
                                                .stay(250)
                                                .to(0.5, 1, 0, 500, IType.SIN_DOWN)
                                                .to(0, 0, 0, 250, IType.SIN_FULL)
                                                .stay(400)
                                                .to(-0.5, 0.5, 0, 150)
                                                .to(-3, -3, 0, 250, IType.SIN_UP))
                                .addBus(
                                        "BULLETS",
                                        new AnimationSequence()
                                                .jump(2, -3, 0)
                                                .stay(250)
                                                .to(0.5, 1, 0, 500, IType.SIN_DOWN)
                                                .to(0, 0, 0, 250, IType.SIN_FULL)
                                                .stay(150)
                                                .to(0, -1.5, 0, 250, IType.SIN_DOWN));
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
                                        "LIFT",
                                        new AnimationSequence()
                                                .stay(350)
                                                .to(-3, 0, 0, 150, IType.SIN_DOWN)
                                                .stayUntil(1050)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "BOLT_TURN",
                                        new AnimationSequence()
                                                .to(0, 0, turn, 150)
                                                .stayUntil(1050)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "BOLT_PULL",
                                        new AnimationSequence()
                                                .stay(100)
                                                .to(0, 0, -1D, 250, IType.SIN_UP)
                                                .stay(500)
                                                .to(0, 0, 0, 200, IType.LINEAR))
                                .addBus(
                                        "BULLET",
                                        mag == 0
                                                ? new AnimationSequence().jump(-100, 0, 0)
                                                : new AnimationSequence()
                                                        .jump(0, 0.1875, 1.5)
                                                        .stay(100)
                                                        .to(0, 0.125, 0.5, 250, IType.SIN_UP)
                                                        .stay(500)
                                                        .to(0, 0.1875, 1.5, 200, IType.LINEAR));
                }

                return null;
            };

    public static void init() {
        Casing.Template casing762 = Casing.template(CasingShape.BOTTLENECK).tints(Casing.BRASS);
        r762_sp =
                new BulletConfig()
                        .setItem(EnumAmmo.R762_SP)
                        .setCasing(CasingType.SMALL, 6)
                        .setCasing(casing762.copy().register("r762"));
        r762_fmj =
                new BulletConfig()
                        .setItem(EnumAmmo.R762_FMJ)
                        .setCasing(CasingType.SMALL, 6)
                        .setDamage(0.8F)
                        .setThresholdNegation(5F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing762.copy().register("r762fmj"));
        r762_jhp =
                new BulletConfig()
                        .setItem(EnumAmmo.R762_JHP)
                        .setCasing(CasingType.SMALL, 6)
                        .setDamage(1.5F)
                        .setHeadshot(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing762.copy().register("r762jhp"));
        r762_ap =
                new BulletConfig()
                        .setItem(EnumAmmo.R762_AP)
                        .setCasing(CasingType.SMALL_STEEL, 6)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.25F)
                        .setThresholdNegation(12.5F)
                        .setArmorPiercing(0.15F)
                        .setCasing(casing762.copy().tints(Casing.MAGNUM_44).register("r762ap"));
        r762_du =
                new BulletConfig()
                        .setItem(EnumAmmo.R762_DU)
                        .setCasing(CasingType.SMALL_STEEL, 6)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.5F)
                        .setThresholdNegation(15F)
                        .setArmorPiercing(0.25F)
                        .setCasing(casing762.copy().tints(Casing.MAGNUM_44).register("r762du"));
        r762_he =
                new BulletConfig()
                        .setItem(EnumAmmo.R762_HE)
                        .setCasing(CasingType.SMALL_STEEL, 6)
                        .setWear(3F)
                        .setDamage(1.75F)
                        .setOnImpact(LAMBDA_TINY_EXPLODE)
                        .setCasing(casing762.copy().tints(Casing.MAGNUM_44).register("r762he"));

        energy_lacunae =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR)
                        .setCasing(
                                () -> new ItemStack(ModItems.ingot(Materials.POLYMER), 2), 4 * 40)
                        .setupDamageClass(ModDamageTypes.LASER)
                        .setBeam()
                        .setReloadCount(40)
                        .setSpread(0.0F)
                        .setLife(5)
                        .setRenderRotations(false)
                        .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
        energy_lacunae_overcharge =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR_OVERCHARGE)
                        .setCasing(
                                () -> new ItemStack(ModItems.ingot(Materials.POLYMER), 2), 4 * 40)
                        .setupDamageClass(ModDamageTypes.LASER)
                        .setBeam()
                        .setReloadCount(40)
                        .setSpread(0.0F)
                        .setLife(5)
                        .setRenderRotations(false)
                        .setDoesPenetrate(true)
                        .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
        energy_lacunae_ir =
                new BulletConfig()
                        .setItem(EnumAmmo.CAPACITOR_IR)
                        .setCasing(
                                () -> new ItemStack(ModItems.ingot(Materials.POLYMER), 2), 4 * 40)
                        .setupDamageClass(ModDamageTypes.FIRE)
                        .setBeam()
                        .setReloadCount(40)
                        .setSpread(0.0F)
                        .setLife(5)
                        .setRenderRotations(false)
                        .setOnBeamImpact(XFactoryEnergy.LAMBDA_IR_HIT);

        ModItems.GUN_CARBINE =
                Registrar.registerItem(
                        "gun_carbine",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(3_000)
                                                        .draw(10)
                                                        .inspect(31)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(15F)
                                                                        .delay(5)
                                                                        .dry(15)
                                                                        .spread(0.0F)
                                                                        .reload(30, 0, 15, 0)
                                                                        .jam(60)
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
                                                                                                14)
                                                                                        .addConfigs(
                                                                                                r762_sp,
                                                                                                r762_fmj,
                                                                                                r762_jhp,
                                                                                                r762_ap,
                                                                                                r762_du,
                                                                                                r762_he))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_CARBINE))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_CARBINE_ANIMS)
                                                        .orchestra(GunAnimationActions.CARBINE))
                                        .setDefaultAmmo(EnumAmmo.R762_SP, 14),
                        Item.Properties::new);

        ModItems.GUN_MINIGUN =
                Registrar.registerItem(
                        "gun_minigun",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(50_000)
                                                        .draw(20)
                                                        .inspect(20)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(6F)
                                                                        .delay(1)
                                                                        .auto(true)
                                                                        .dry(15)
                                                                        .spread(0.01F)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_MINIGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineBelt()
                                                                                        .addConfigs(
                                                                                                r762_sp,
                                                                                                r762_fmj,
                                                                                                r762_jhp,
                                                                                                r762_ap,
                                                                                                r762_du,
                                                                                                r762_he))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MINIGUN))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_MINIGUN_ANIMS)
                                                        .orchestra(GunAnimationActions.MINIGUN))
                                        .setDefaultAmmo(EnumAmmo.R762_FMJ, 30),
                        Item.Properties::new);

        ModItems.GUN_MINIGUN_LACUNAE =
                Registrar.registerItem(
                        "gun_minigun_lacunae",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                GunConfig.builder()
                                                        .dura(50_000)
                                                        .draw(20)
                                                        .inspect(20)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(12F)
                                                                        .delay(1)
                                                                        .auto(true)
                                                                        .dry(15)
                                                                        .reload(15)
                                                                        .spread(0.01F)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_LASER_GATLING_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                200)
                                                                                        .addConfigs(
                                                                                                energy_lacunae,
                                                                                                energy_lacunae_overcharge,
                                                                                                energy_lacunae_ir))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_LACUNAE))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_MINIGUN_ANIMS)
                                                        .orchestra(GunAnimationActions.MINIGUN))
                                        .setDefaultAmmo(EnumAmmo.CAPACITOR, 15),
                        Item.Properties::new);

        ModItems.GUN_MINIGUN_DUAL =
                Registrar.registerItem(
                        "gun_minigun_dual",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.DEBUG,
                                                props,
                                                GunConfig.builder()
                                                        .dura(50_000)
                                                        .draw(20)
                                                        .inspect(20)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(6F)
                                                                        .delay(1)
                                                                        .auto(true)
                                                                        .dry(15)
                                                                        .spread(0.01F)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_MINIGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineBelt()
                                                                                        .addConfigs(
                                                                                                r762_sp,
                                                                                                r762_fmj,
                                                                                                r762_jhp,
                                                                                                r762_ap,
                                                                                                r762_du,
                                                                                                r762_he))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MINIGUN))
                                                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(
                                                                GunStateDecider
                                                                        .LAMBDA_STANDARD_DECIDER)
                                                        .anim(LAMBDA_MINIGUN_ANIMS)
                                                        .orchestra(
                                                                GunAnimationActions.MINIGUN_DUAL),
                                                GunConfig.builder()
                                                        .dura(50_000)
                                                        .draw(20)
                                                        .inspect(20)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(6F)
                                                                        .delay(1)
                                                                        .auto(true)
                                                                        .dry(15)
                                                                        .spread(0.01F)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_MINIGUN_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineBelt()
                                                                                        .addConfigs(
                                                                                                r762_sp,
                                                                                                r762_fmj,
                                                                                                r762_jhp,
                                                                                                r762_ap,
                                                                                                r762_du,
                                                                                                r762_he))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MINIGUN))
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(LAMBDA_SECOND_MINIGUN)
                                                        .anim(LAMBDA_MINIGUN_ANIMS)
                                                        .orchestra(
                                                                GunAnimationActions.MINIGUN_DUAL))
                                        .setDefaultAmmo(EnumAmmo.R762_SP, 50),
                        Item.Properties::new);

        ModItems.GUN_MAS36 =
                Registrar.registerItem(
                        "gun_mas36",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                GunConfig.builder()
                                                        .dura(5_000)
                                                        .draw(20)
                                                        .inspect(31)
                                                        .reloadSequential(true)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(30F)
                                                                        .delay(25)
                                                                        .dry(25)
                                                                        .spread(0.0F)
                                                                        .reload(43)
                                                                        .jam(43)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_HEAVY_RIFLE_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                7)
                                                                                        .addConfigs(
                                                                                                r762_sp,
                                                                                                r762_fmj,
                                                                                                r762_jhp,
                                                                                                r762_ap,
                                                                                                r762_du,
                                                                                                r762_he))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_CARBINE))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_MAS36_ANIMS)
                                                        .orchestra(GunAnimationActions.MAS36))
                                        .setDefaultAmmo(EnumAmmo.R762_AP, 14),
                        Item.Properties::new);
    }
}

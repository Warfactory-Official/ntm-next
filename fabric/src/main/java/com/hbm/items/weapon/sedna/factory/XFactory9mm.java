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
import com.hbm.items.ModItems;
import com.hbm.items.variant.CasingType;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.main.ResourceManager;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.stats.ModStats;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class XFactory9mm {

    public static BulletConfig p9_sp;
    public static BulletConfig p9_fmj;
    public static BulletConfig p9_jhp;
    public static BulletConfig p9_ap;
    public static Function<ItemStack, String> LAMBDA_NAME_GREASEGUN =
            (stack) -> {
                if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_GREASEGUN_CLEAN))
                    return stack.getItem().getDescriptionId() + "_m3";
                return null;
            };
    public static Function<ItemStack, String> LAMBDA_NAME_UZI =
            (stack) -> {
                if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER))
                    return stack.getItem().getDescriptionId() + "_richter";
                return null;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_GREASEGUN =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        2, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_UZI =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        1, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_LAG =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE_LAG =
            (stack, ctx) -> {
                GunAnimation type = GunBaseNTItem.getLastAnim(stack, ctx.configIndex());
                int timer = GunBaseNTItem.getAnimTimer(stack, ctx.configIndex());
                Player player = ctx.getPlayer();
                if (player != null && type == GunAnimation.INSPECT && timer > 20 && timer < 60) {
                    int index = ctx.configIndex();
                    Receiver primary = ctx.config().getReceivers(stack).get(0);
                    Magazine mag = primary.getMagazine(stack);
                    BulletConfig config = (BulletConfig) mag.getType(stack, ctx.inventory());
                    player.awardStat(ModStats.BULLETS.get());
                    mag.consumeAmmo(stack, ctx.inventory(), 1);
                    GunBaseNTItem.setWear(
                            stack,
                            index,
                            Math.min(
                                    GunBaseNTItem.getWear(stack, index) + config.wear,
                                    ctx.config().getDurability(stack)));
                    if (primary.getFireSound(stack) != null)
                        EntitySounds.play(
                                player,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                primary.getFireSound(stack).get(),
                                SoundSource.PLAYERS,
                                primary.getFireVolume(stack),
                                primary.getFirePitch(stack));
                    GunBaseNTItem.setState(stack, index, GunState.COOLDOWN);
                    GunBaseNTItem.setTimer(stack, index, primary.getDelayAfterFire(stack));
                    if (player.level() instanceof ServerLevel server) {
                        player.hurtServer(
                                server,
                                ModifiedDamageSource.of(
                                        BulletConfig.getDamage(
                                                player, player, ModDamageTypes.PHYSICAL),
                                        true,
                                        1D,
                                        5F,
                                        0F),
                                1_000F);
                    }
                } else {
                    Lego.doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);
                }
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LAG_ANIMS =
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
                        return ResourceManager.lag_anim.get("Firing");
                    case CYCLE_DRY:
                        return ResourceManager.lag_anim.get("Dryfire");
                    case RELOAD:
                        return ResourceManager.lag_anim.get("Reload");
                    case JAMMED:
                        return ResourceManager.lag_anim.get("Jam");
                    case INSPECT:
                        return ResourceManager.lag_anim
                                .get("Inspect")
                                .addBus(
                                        "ADD_TRANS",
                                        new AnimationSequence()
                                                .to(-4, 0, -3, 500)
                                                .to(-4, 0, -3, 2000)
                                                .to(0, 0, 0, 500))
                                .addBus(
                                        "ADD_ROT",
                                        new AnimationSequence()
                                                .to(0, -2, 5, 500)
                                                .to(0, -2, 5, 2000)
                                                .to(0, 0, 0, 500));
                }

                return null;
            };

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_UZI =
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
                Lego.handleStandardSmoke(ctx.entity(), stack, 2000, 0.05D, 1.1D, ctx.configIndex());
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_GREASEGUN_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(80, 0, 0, 0)
                                                .to(80, 0, 0, 500)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "STOCK",
                                        new AnimationSequence()
                                                .to(0, 0, -4, 0)
                                                .to(0, 0, -4, 200)
                                                .to(0, 0, 0, 300, IType.SIN_FULL));
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
                                        "FLAP",
                                        new AnimationSequence()
                                                .to(0, 0, 15, 100, IType.SIN_DOWN)
                                                .to(0, 0, -5, 100, IType.SIN_FULL)
                                                .to(0, 0, 0, 50, IType.SIN_FULL));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 750)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, -45, 250, IType.SIN_FULL)
                                                .to(0, 0, -45, 750)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "HANDLE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 0, 250)
                                                .to(-90, 0, 0, 250, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL));
                    case RELOAD:
                        boolean empty =
                                ((GunBaseNTItem) stack.getItem())
                                                .getConfig(stack, 0)
                                                .getReceivers(stack)
                                                .get(0)
                                                .getMagazine(stack)
                                                .getAmount(
                                                        stack,
                                                        ClientPlayerAccess.player().getInventory())
                                        <= 0;
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, -8, 0, 250, IType.SIN_UP)
                                                .to(0, -8, 0, 750)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 1750)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1750)
                                                .to(0, 0, -45, 250, IType.SIN_FULL)
                                                .to(0, 0, -45, 500)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "HANDLE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 2000)
                                                .to(-90, 0, 0, 250, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .to(empty ? 1 : 0, 0, 0, 0)
                                                .to(0, 0, 0, 1000));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 1500)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, -45, 250, IType.SIN_FULL)
                                                .to(0, 0, -45, 1500)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "HANDLE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 0, 250)
                                                .to(-90, 0, 0, 250, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL)
                                                .to(0, 0, 0, 250)
                                                .to(-90, 0, 0, 250, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_FULL));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "TURN",
                                        new AnimationSequence()
                                                .to(0, 0, -45, 150)
                                                .to(0, 0, 45, 150)
                                                .to(0, 0, 45, 50)
                                                .to(0, 0, 0, 250)
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 45, 150)
                                                .to(0, 0, -45, 150)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "FLAP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 300)
                                                .to(0, 0, 180, 150)
                                                .to(0, 0, 180, 850)
                                                .to(0, 0, 0, 150));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_UZI_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(80, 0, 0, 0)
                                                .to(80, 0, 0, 500)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "STOCKBACK",
                                        new AnimationSequence()
                                                .to(-200, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "STOCKFRONT",
                                        new AnimationSequence()
                                                .to(180, 0, 0, 0)
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
                                                                ? -0.5
                                                                : -0.75,
                                                        25,
                                                        IType.SIN_DOWN)
                                                .to(0, 0, 0, 75, IType.SIN_FULL));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 500)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, -2, 150, IType.SIN_FULL)
                                                .to(0, 0, 0, 50, IType.SIN_UP));
                    case RELOAD:
                        boolean empty =
                                ((GunBaseNTItem) stack.getItem())
                                                .getConfig(stack, 0)
                                                .getReceivers(stack)
                                                .get(0)
                                                .getMagazine(stack)
                                                .getAmount(
                                                        stack,
                                                        ClientPlayerAccess.player().getInventory())
                                        <= 0;
                        return new BusAnimation()
                                .addBus(
                                        "MAG",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(0, -10, 0, 250, IType.SIN_UP)
                                                .to(0, -10, 0, 750)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 2000)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 2000)
                                                .to(0, 0, -2, 150, IType.SIN_FULL)
                                                .to(0, 0, 0, 50, IType.SIN_UP))
                                .addBus(
                                        "BULLET",
                                        new AnimationSequence()
                                                .to(empty ? 0 : 1, 0, 0, 0)
                                                .to(empty ? 0 : 1, 0, 0, 500)
                                                .to(1, 0, 0, 0));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(-25, 0, 0, 250, IType.SIN_FULL)
                                                .to(-25, 0, 0, 1250)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "SLIDE",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1000)
                                                .to(0, 0, -2, 150, IType.SIN_FULL)
                                                .to(0, 0, 0, 50, IType.SIN_UP)
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, -2, 150, IType.SIN_FULL)
                                                .to(0, 0, 0, 50, IType.SIN_UP));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "YEET",
                                        new AnimationSequence()
                                                .to(0, -1, 0, 100)
                                                .to(0, 0, 0, 100, IType.SIN_UP)
                                                .to(0, 12, 0, 350, IType.SIN_DOWN)
                                                .to(0, 0, 0, 350, IType.SIN_UP)
                                                .to(0, -1, 0, 50, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "SPEEN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(-360, 0, 0, 600));
                }
                return null;
            };

    public static void init() {
        Casing.Template casing9 =
                Casing.template(CasingShape.STRAIGHT).tints(Casing.BRASS).scale(1F, 1F, 0.75F);
        p9_sp =
                new BulletConfig()
                        .setItem(EnumAmmo.P9_SP)
                        .setCasing(CasingType.SMALL, 12)
                        .setCasing(casing9.copy().register("p9"));
        p9_fmj =
                new BulletConfig()
                        .setItem(EnumAmmo.P9_FMJ)
                        .setCasing(CasingType.SMALL, 12)
                        .setDamage(0.8F)
                        .setThresholdNegation(2F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing9.copy().register("p9fmj"));
        p9_jhp =
                new BulletConfig()
                        .setItem(EnumAmmo.P9_JHP)
                        .setCasing(CasingType.SMALL, 12)
                        .setDamage(1.5F)
                        .setHeadshot(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing9.copy().register("p9jhp"));
        p9_ap =
                new BulletConfig()
                        .setItem(EnumAmmo.P9_AP)
                        .setCasing(CasingType.SMALL_STEEL, 12)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.25F)
                        .setThresholdNegation(5F)
                        .setArmorPiercing(0.15F)
                        .setCasing(casing9.copy().tints(Casing.MAGNUM_44).register("p9ap"));

        ModItems.GUN_GREASEGUN =
                Registrar.registerItem(
                        "gun_greasegun",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(3_000)
                                                        .draw(20)
                                                        .inspect(31)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(3F)
                                                                        .delay(4)
                                                                        .dry(40)
                                                                        .auto(true)
                                                                        .spread(0.015F)
                                                                        .reload(60)
                                                                        .jam(55)
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
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                p9_sp,
                                                                                                p9_fmj,
                                                                                                p9_jhp,
                                                                                                p9_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_GREASEGUN))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_GREASEGUN_ANIMS)
                                                        .orchestra(GunAnimationActions.GREASEGUN))
                                        .setDefaultAmmo(EnumAmmo.P9_SP, 30)
                                        .setNameMutator(LAMBDA_NAME_GREASEGUN),
                        Item.Properties::new);

        ModItems.GUN_LAG =
                Registrar.registerItem(
                        "gun_lag",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(1_700)
                                                        .draw(7)
                                                        .inspect(31)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(25F)
                                                                        .delay(4)
                                                                        .dry(10)
                                                                        .spread(0.005F)
                                                                        .reload(53)
                                                                        .jam(44)
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
                                                                                                17)
                                                                                        .addConfigs(
                                                                                                p9_sp,
                                                                                                p9_fmj,
                                                                                                p9_jhp,
                                                                                                p9_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .fire(LAMBDA_FIRE_LAG)
                                                                        .recoil(LAMBDA_RECOIL_LAG))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_LAG_ANIMS)
                                                        .orchestra(GunAnimationActions.LAG))
                                        .setDefaultAmmo(EnumAmmo.P9_JHP, 17),
                        Item.Properties::new);

        ModItems.GUN_UZI =
                Registrar.registerItem(
                        "gun_uzi",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(3_000)
                                                        .draw(15)
                                                        .inspect(31)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(3F)
                                                                        .delay(2)
                                                                        .dry(25)
                                                                        .auto(true)
                                                                        .spread(0.005F)
                                                                        .reload(55)
                                                                        .jam(50)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_UZI_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                p9_sp,
                                                                                                p9_fmj,
                                                                                                p9_jhp,
                                                                                                p9_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_UZI))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_UZI_ANIMS)
                                                        .orchestra(GunAnimationActions.UZI))
                                        .setDefaultAmmo(EnumAmmo.P9_SP, 30)
                                        .setNameMutator(LAMBDA_NAME_UZI),
                        Item.Properties::new);

        ModItems.GUN_UZI_AKIMBO =
                Registrar.registerItem(
                        "gun_uzi_akimbo",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(3_000)
                                                        .draw(15)
                                                        .inspect(31)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(3F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(2)
                                                                        .dry(25)
                                                                        .auto(true)
                                                                        .spread(0.005F)
                                                                        .reload(55)
                                                                        .jam(50)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_UZI_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                p9_sp,
                                                                                                p9_fmj,
                                                                                                p9_jhp,
                                                                                                p9_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                0.375D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_UZI))
                                                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(
                                                                GunStateDecider
                                                                        .LAMBDA_STANDARD_DECIDER)
                                                        .anim(LAMBDA_UZI_ANIMS)
                                                        .orchestra(GunAnimationActions.UZI_AKIMBO),
                                                GunConfig.builder()
                                                        .dura(3_000)
                                                        .draw(15)
                                                        .inspect(31)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(3F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(2)
                                                                        .dry(25)
                                                                        .auto(true)
                                                                        .spread(0.005F)
                                                                        .reload(55)
                                                                        .jam(50)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_UZI_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                1,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                p9_sp,
                                                                                                p9_fmj,
                                                                                                p9_jhp,
                                                                                                p9_ap))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.375D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_UZI))
                                                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(LAMBDA_SECOND_UZI)
                                                        .anim(LAMBDA_UZI_ANIMS)
                                                        .orchestra(GunAnimationActions.UZI_AKIMBO))
                                        .setDefaultAmmo(EnumAmmo.P9_SP, 60),
                        Item.Properties::new);
    }
}

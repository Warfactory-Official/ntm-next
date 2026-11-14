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

import com.hbm.entity.projectile.BulletBeamBaseEntity;
import com.hbm.entity.projectile.CoinEntity;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.impl.GunNI4NIItem;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineInfinite;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.ModDamageTypes;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class XFactoryAccelerator {

    public static MagazineBelt tauChargeMag = new MagazineBelt();

    public static BulletConfig tau_uranium;
    public static BulletConfig tau_uranium_charge;

    public static BulletConfig coil_tungsten;
    public static BulletConfig coil_ferrouranium;

    public static BulletConfig ni4ni_arc;

    public static Consumer<Entity> LAMBDA_UPDATE_TUNGSTEN =
            (entity) -> {
                breakInPath(entity, 1.25F);
            };
    public static Consumer<Entity> LAMBDA_UPDATE_FERRO =
            (entity) -> {
                breakInPath(entity, 2.5F);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TAU_PRIMARY_RELEASE =
            (stack, ctx) -> {
                if (ctx.getPlayer() == null
                        || GunBaseNTItem.getLastAnim(stack, ctx.configIndex())
                                != GunAnimation.CYCLE) return;
                WorldSounds.play(
                        ctx.getPlayer().level(),
                        null,
                        ctx.getPlayer().getX(),
                        ctx.getPlayer().getY(),
                        ctx.getPlayer().getZ(),
                        ModSounds.GUN_TAU_STOPFIRE.get(),
                        SoundSource.PLAYERS,
                        1F,
                        1F);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TAU_SECONDARY_PRESS =
            (stack, ctx) -> {
                if (ctx.getPlayer() == null) return;
                if (ctx.config()
                                .getReceivers(stack)
                                .get(0)
                                .getMagazine(stack)
                                .getAmount(stack, ctx.inventory())
                        <= 0) return;
                GunBaseNTItem.playAnimation(
                        ctx.getPlayer(), stack, GunAnimation.SPINUP, ctx.configIndex());
                MagazineBelt.getMagType(stack);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TAU_SECONDARY_RELEASE =
            (stack, ctx) -> {
                if (ctx.getPlayer() == null) return;
                int timer = GunBaseNTItem.getAnimTimer(stack, ctx.configIndex());

                if (timer >= 10
                        && GunBaseNTItem.getLastAnim(stack, ctx.configIndex())
                                == GunAnimation.SPINUP) {
                    GunBaseNTItem.playAnimation(
                            ctx.getPlayer(), stack, GunAnimation.ALT_CYCLE, ctx.configIndex());
                    int unitsUsed = 1 + Math.min(12, timer / 10);

                    LivingEntity entity = ctx.entity();
                    int index = ctx.configIndex();

                    Receiver primary = ctx.config().getReceivers(stack).get(0);
                    BulletConfig config = tauChargeMag.getFirstConfig(stack, ctx.inventory());

                    Vec3 offset = primary.getProjectileOffset(stack);
                    double forwardOffset = offset.x;
                    double heightOffset = offset.y;
                    double sideOffset = offset.z;

                    float damage =
                            Lego.getStandardWearDamage(stack, ctx.config(), index) * unitsUsed * 5;
                    float spread = Lego.calcSpread(ctx, stack, primary, config, true, index, false);
                    BulletBeamBaseEntity mk4 =
                            new BulletBeamBaseEntity(
                                    entity,
                                    config,
                                    damage,
                                    spread,
                                    sideOffset,
                                    heightOffset,
                                    forwardOffset);
                    entity.level().addFreshEntity(mk4);

                    GunBaseNTItem.setWear(
                            stack,
                            index,
                            Math.min(
                                    GunBaseNTItem.getWear(stack, index) + config.wear * unitsUsed,
                                    ctx.config().getDurability(stack)));

                } else {
                    GunBaseNTItem.playAnimation(
                            ctx.getPlayer(), stack, GunAnimation.CYCLE_DRY, ctx.configIndex());
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_NI4NI_SECONDARY_PRESS =
            (stack, ctx) -> {
                if (ctx.getPlayer() == null) return;
                Player player = ctx.getPlayer();

                if (GunNI4NIItem.getCoinCount(stack) > 0) {
                    Vec3 vec = player.getLookAngle().scale(0.8D);
                    CoinEntity coin = new CoinEntity(player.level());
                    coin.setPos(
                            player.getX(),
                            player.getY() + player.getEyeHeight() - 0.5 - 0.125,
                            player.getZ());
                    coin.setDeltaMovement(vec.x, vec.y + 0.5, vec.z);
                    coin.setYRot(player.getYRot());
                    coin.setThrower(player);
                    player.level().addFreshEntity(coin);

                    EntitySounds.play(
                            player,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS,
                            1.0F,
                            1F + player.getRandom().nextFloat() * 0.25F);

                    GunNI4NIItem.setCoinCount(stack, GunNI4NIItem.getCoinCount(stack) - 1);
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_TAU = (stack, ctx) -> {};
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_COILGUN =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_TAU_ANIMS =
            (stack, type) -> {
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
                                                .to(0, 0, -0.5, 50)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, -5, 50, IType.SIN_DOWN)
                                                .to(0, 0, 5, 100, IType.SIN_FULL)
                                                .to(0, 0, 0, 50, IType.SIN_UP));
                    case ALT_CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, -3, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, -5, 50, IType.SIN_DOWN)
                                                .to(0, 0, 5, 100, IType.SIN_FULL)
                                                .to(0, 0, 0, 50, IType.SIN_UP));
                    case CYCLE_DRY:
                        return new BusAnimation();
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(2, 0, 0, 150, IType.SIN_DOWN)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, -360 * 3, 500 * 3, IType.SIN_DOWN));
                    case SPINUP:
                        return new BusAnimation()
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, 360 * 6, 3000, IType.SIN_UP)
                                                .to(0, 0, 0, 0)
                                                .to(0, 0, 360 * 40, 500 * 20));
                }

                return null;
            };

    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_COILGUN_ANIMS =
            (stack, type) -> {
                if (type == GunAnimation.EQUIP)
                    return new BusAnimation()
                            .addBus(
                                    "RELOAD",
                                    new AnimationSequence().to(1, 0, 0, 0).to(0, 0, 0, 250));
                if (type == GunAnimation.CYCLE)
                    return new BusAnimation()
                            .addBus(
                                    "RECOIL",
                                    new AnimationSequence()
                                            .to(
                                                    GunBaseNTItem.getIsAiming(stack) ? 0.5 : 1,
                                                    0,
                                                    0,
                                                    100)
                                            .to(0, 0, 0, 200));
                if (type == GunAnimation.RELOAD)
                    return new BusAnimation()
                            .addBus(
                                    "RELOAD",
                                    new AnimationSequence()
                                            .to(1, 0, 0, 250)
                                            .to(1, 0, 0, 500)
                                            .to(0, 0, 0, 250));
                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_NI4NI_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus("EQUIP", new AnimationSequence().to(-360 * 2, 0, 0, 500));
                    case CYCLE:
                        boolean aiming = GunBaseNTItem.getIsAiming(stack);
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(aiming ? -5 : -30, 0, 0, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "DRUM",
                                        new AnimationSequence()
                                                .stay(50)
                                                .to(0, 0, 120, 300, IType.SIN_FULL));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(-360 * 3, 0, 0, 750)
                                                .stay(100)
                                                .to(0, 0, 0, 750));
                }

                return null;
            };

    public static void breakInPath(Entity entity, float threshold) {

        Vec3 vec =
                new Vec3(
                        entity.getX() - entity.xOld,
                        entity.getY() - entity.yOld,
                        entity.getZ() - entity.zOld);
        double motion = Math.max(vec.length(), 0.1);
        vec = vec.normalize();

        for (double d = 0; d < motion; d += 0.5) {

            double dX = entity.getX() - vec.x * d;
            double dY = entity.getY() - vec.y * d;
            double dZ = entity.getZ() - vec.z * d;

            if (entity.level().isClientSide()) {
                entity.level().addParticle(ParticleTypes.FIREWORK, dX, dY, dZ, 0, 0, 0);
            } else {
                BlockPos pos = BlockPos.containing(dX, dY, dZ);
                BlockState state = entity.level().getBlockState(pos);
                float hardness = state.getDestroySpeed(entity.level(), pos);
                if (!state.isAir() && hardness >= 0 && hardness < threshold) {
                    entity.level().destroyBlock(pos, false);
                }
            }
        }
    }

    public static void init() {

        tau_uranium =
                new BulletConfig()
                        .setItem(EnumAmmo.TAU_URANIUM)
                        .setCasing(() -> new ItemStack(ModItems.plate(Materials.LEAD), 2), 16)
                        .setupDamageClass(ModDamageTypes.SUBATOMIC)
                        .setBeam()
                        .setLife(5)
                        .setRenderRotations(false)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setOnBeamImpact(BulletConfig.LAMBDA_BEAM_HIT);
        tau_uranium_charge =
                new BulletConfig()
                        .setItem(EnumAmmo.TAU_URANIUM)
                        .setCasing(() -> new ItemStack(ModItems.plate(Materials.LEAD), 2), 16)
                        .setupDamageClass(ModDamageTypes.SUBATOMIC)
                        .setBeam()
                        .setLife(5)
                        .setRenderRotations(false)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setSpectral(true)
                        .setOnBeamImpact(BulletConfig.LAMBDA_BEAM_HIT);

        coil_tungsten =
                new BulletConfig()
                        .setItem(EnumAmmo.COIL_TUNGSTEN)
                        .setVel(7.5F)
                        .setLife(50)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setSpectral(true)
                        .setOnUpdate(LAMBDA_UPDATE_TUNGSTEN);
        coil_ferrouranium =
                new BulletConfig()
                        .setItem(EnumAmmo.COIL_FERROURANIUM)
                        .setVel(7.5F)
                        .setLife(50)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setSpectral(true)
                        .setOnUpdate(LAMBDA_UPDATE_FERRO);

        ni4ni_arc =
                new BulletConfig()
                        .setupDamageClass(ModDamageTypes.PHYSICAL)
                        .setBeam()
                        .setLife(5)
                        .setThresholdNegation(10F)
                        .setArmorPiercing(0.2F)
                        .setRenderRotations(false)
                        .setDoesPenetrate(false)
                        .setOnBeamImpact(BulletConfig.LAMBDA_BEAM_HIT);

        tauChargeMag.addConfigs(tau_uranium_charge);

        ModItems.GUN_TAU =
                Registrar.registerItem(
                        "gun_tau",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(6_400)
                                                        .draw(10)
                                                        .inspect(10)
                                                        .crosshair(Crosshair.CIRCLE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(25F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(4)
                                                                        .auto(true)
                                                                        .spread(0F)
                                                                        .mag(
                                                                                new MagazineBelt()
                                                                                        .addConfigs(
                                                                                                tau_uranium))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 2.5,
                                                                                -0.25D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_TAU))
                                                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                        .rp(LAMBDA_TAU_PRIMARY_RELEASE)
                                                        .ps(LAMBDA_TAU_SECONDARY_PRESS)
                                                        .rs(LAMBDA_TAU_SECONDARY_RELEASE)
                                                        .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                        .decider(
                                                                GunStateDecider
                                                                        .LAMBDA_STANDARD_DECIDER)
                                                        .anim(LAMBDA_TAU_ANIMS)
                                                        .orchestra(GunAnimationActions.TAU))
                                        .setDefaultAmmo(EnumAmmo.TAU_URANIUM, 15),
                        Item.Properties::new);

        ModItems.GUN_COILGUN =
                Registrar.registerItem(
                        "gun_coilgun",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.SPECIAL,
                                                props,
                                                GunConfig.builder()
                                                        .dura(400)
                                                        .draw(5)
                                                        .inspect(39)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(35F)
                                                                        .delay(5)
                                                                        .reload(20)
                                                                        .jam(33)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_COIL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                1)
                                                                                        .addConfigs(
                                                                                                coil_tungsten,
                                                                                                coil_ferrouranium))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_COILGUN))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_COILGUN_ANIMS)
                                                        .orchestra(GunAnimationActions.COILGUN))
                                        .setDefaultAmmo(EnumAmmo.COIL_TUNGSTEN, 5),
                        Item.Properties::new);

        ModItems.GUN_N_I_4_N_I =
                Registrar.registerItem(
                        "gun_n_i_4_n_i",
                        props ->
                                new GunNI4NIItem(
                                        WeaponQuality.SPECIAL,
                                        props,
                                        GunConfig.builder()
                                                .dura(0)
                                                .draw(5)
                                                .inspect(39)
                                                .crosshair(Crosshair.CIRCLE)
                                                .rec(
                                                        Receiver.builder()
                                                                .dmg(35F)
                                                                .delay(10)
                                                                .sound(
                                                                        () ->
                                                                                ModSounds
                                                                                        .GUN_COIL_FIRE
                                                                                        .get(),
                                                                        1.0F,
                                                                        1.0F)
                                                                .mag(
                                                                        new MagazineInfinite(
                                                                                ni4ni_arc))
                                                                .offset(0.75, -0.0625, -0.1875D)
                                                                .setupStandardFire()
                                                                .fire(Lego.LAMBDA_NOWEAR_FIRE))
                                                .setupStandardConfiguration()
                                                .ps(LAMBDA_NI4NI_SECONDARY_PRESS)
                                                .anim(LAMBDA_NI4NI_ANIMS)
                                                .orchestra(GunAnimationActions.NI4NI)),
                        Item.Properties::new);
    }
}

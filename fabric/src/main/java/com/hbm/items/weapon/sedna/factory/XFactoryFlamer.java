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
import com.hbm.entity.effect.FireLingeringEntity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.extprop.LivingHazards;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.impl.GunChemthrowerItem;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.ModDamageTypes;
import com.hbm.main.ResourceManager;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.platform.Platform;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.ModSounds;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public class XFactoryFlamer {

    public static BulletConfig flame_nograv;
    public static BulletConfig flame_nograv_bf;

    public static BulletConfig flame_diesel;
    public static BulletConfig flame_gas;
    public static BulletConfig flame_napalm;
    public static BulletConfig flame_balefire;

    public static BulletConfig flame_topaz_diesel;
    public static BulletConfig flame_topaz_gas;
    public static BulletConfig flame_topaz_napalm;
    public static BulletConfig flame_topaz_balefire;

    public static BulletConfig flame_daybreaker_diesel;
    public static BulletConfig flame_daybreaker_gas;
    public static BulletConfig flame_daybreaker_napalm;
    public static BulletConfig flame_daybreaker_balefire;

    public static Consumer<Entity> LAMBDA_FIRE =
            (bullet) -> {
                if (bullet.level().isClientSide() && nearClientPlayer(bullet))
                    FlameCreator.composeEffectClient(
                            bullet.level(),
                            bullet.getX(),
                            bullet.getY() - 0.125,
                            bullet.getZ(),
                            FlameCreator.META_FIRE);
            };
    public static Consumer<Entity> LAMBDA_BALEFIRE =
            (bullet) -> {
                if (bullet.level().isClientSide() && nearClientPlayer(bullet))
                    FlameCreator.composeEffectClient(
                            bullet.level(),
                            bullet.getX(),
                            bullet.getY() - 0.125,
                            bullet.getZ(),
                            FlameCreator.META_BALEFIRE);
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_IGNITE_FIRE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    LivingHazards props = LivingHazards.getOrCreate(living);
                    if (props.fire < 100) props.fire = 100;
                }
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_IGNITE_BALEFIRE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    LivingHazards props = LivingHazards.getOrCreate(living);
                    if (props.balefire < 200) props.balefire = 200;
                }
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_LINGER_DIESEL =
            (bullet, mop) -> {
                if (!igniteIfPossible(bullet, mop))
                    spawnFire(bullet, mop, 2F, 1F, 100, FireLingeringEntity.TYPE_DIESEL);
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_LINGER_GAS =
            (bullet, mop) -> {
                igniteIfPossible(bullet, mop);
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_LINGER_NAPALM =
            (bullet, mop) -> {
                if (!igniteIfPossible(bullet, mop))
                    spawnFire(bullet, mop, 2.5F, 1F, 200, FireLingeringEntity.TYPE_DIESEL);
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_LINGER_BALEFIRE =
            (bullet, mop) -> {
                spawnFire(bullet, mop, 3F, 1F, 300, FireLingeringEntity.TYPE_BALEFIRE);
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FLAMER_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(-45, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN));
                    case RELOAD:
                        return ResourceManager.flamethrower_anim.get("Reload");
                    case INSPECT:
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, 45, 250, IType.SIN_FULL)
                                                .to(0, 0, 45, 350)
                                                .to(0, 0, -15, 150, IType.SIN_FULL)
                                                .to(0, 0, 0, 100, IType.SIN_FULL));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CHEMTHROWER_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(-45, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN));
                }

                return null;
            };

    private static boolean nearClientPlayer(Entity bullet) {
        Player player = ClientPlayerAccess.player();
        return player != null && player.getEyePosition().distanceTo(bullet.position()) < 100;
    }

    public static boolean igniteIfPossible(BulletBaseMK4Entity bullet, HitResult mop) {
        if (mop instanceof BlockHitResult blockHit) {
            Level world = bullet.level();
            BlockPos pos = blockHit.getBlockPos();
            Direction dir = blockHit.getDirection();
            BlockState state = world.getBlockState(pos);
            if (Platform.isFlammable(world, pos, state, dir)) {
                BlockPos target = pos.relative(dir);
                if (world.getBlockState(target).isAir()) {
                    world.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                    return true;
                }
            }
            bullet.discard();
        }
        return false;
    }

    public static void spawnFire(
            BulletBaseMK4Entity bullet,
            HitResult mop,
            float width,
            float height,
            int duration,
            int type) {
        if (mop instanceof BlockHitResult) {
            Vec3 hit = mop.getLocation();
            List<FireLingeringEntity> fires =
                    bullet.level()
                            .getEntitiesOfClass(
                                    FireLingeringEntity.class,
                                    new AABB(hit.x, hit.y, hit.z, hit.x, hit.y, hit.z)
                                            .inflate(
                                                    width / 2 + 0.5,
                                                    height / 2 + 0.5,
                                                    width / 2 + 0.5));
            if (fires.isEmpty()) {
                FireLingeringEntity fire =
                        new FireLingeringEntity(bullet.level())
                                .setArea(width, height)
                                .setDuration(duration)
                                .setType(type);
                fire.setPos(hit);
                bullet.level().addFreshEntity(fire);
            }
            bullet.discard();
        }
    }

    private static ItemStack plateSteel(int count) {
        return new ItemStack(
                BuiltInRegistries.ITEM.getOptional(RegistryUtil.id("plate_steel")).orElseThrow(),
                count);
    }

    public static void init() {
        flame_diesel =
                new BulletConfig()
                        .setItem(EnumAmmo.FLAME_DIESEL)
                        .setCasing(() -> plateSteel(2), 500)
                        .setupDamageClass(ModDamageTypes.FIRE)
                        .setLife(100)
                        .setVel(1F)
                        .setGrav(0.02D)
                        .setReloadCount(500)
                        .setSelfDamageDelay(20)
                        .setKnockback(0F)
                        .setOnImpact(LAMBDA_IGNITE_FIRE)
                        .setOnUpdate(LAMBDA_FIRE)
                        .setOnRicochet(LAMBDA_LINGER_DIESEL);
        flame_gas =
                new BulletConfig()
                        .setItem(EnumAmmo.FLAME_GAS)
                        .setCasing(() -> plateSteel(2), 500)
                        .setupDamageClass(ModDamageTypes.FIRE)
                        .setLife(10)
                        .setSpread(0.05F)
                        .setVel(1F)
                        .setGrav(0.0D)
                        .setReloadCount(500)
                        .setSelfDamageDelay(20)
                        .setKnockback(0F)
                        .setOnImpact(LAMBDA_IGNITE_FIRE)
                        .setOnUpdate(LAMBDA_FIRE)
                        .setOnRicochet(LAMBDA_LINGER_GAS);
        flame_napalm =
                new BulletConfig()
                        .setItem(EnumAmmo.FLAME_NAPALM)
                        .setCasing(() -> plateSteel(2), 500)
                        .setupDamageClass(ModDamageTypes.FIRE)
                        .setLife(200)
                        .setVel(1F)
                        .setGrav(0.02D)
                        .setReloadCount(500)
                        .setSelfDamageDelay(20)
                        .setKnockback(0F)
                        .setOnImpact(LAMBDA_IGNITE_FIRE)
                        .setOnUpdate(LAMBDA_FIRE)
                        .setOnRicochet(LAMBDA_LINGER_NAPALM);
        flame_balefire =
                new BulletConfig()
                        .setItem(EnumAmmo.FLAME_BALEFIRE)
                        .setCasing(() -> plateSteel(2), 500)
                        .setupDamageClass(ModDamageTypes.FIRE)
                        .setLife(200)
                        .setVel(1F)
                        .setGrav(0.02D)
                        .setReloadCount(500)
                        .setSelfDamageDelay(20)
                        .setKnockback(0F)
                        .setOnImpact(LAMBDA_IGNITE_BALEFIRE)
                        .setOnUpdate(LAMBDA_BALEFIRE)
                        .setOnRicochet(LAMBDA_LINGER_BALEFIRE);

        flame_nograv = flame_diesel.clone().setGrav(0);
        flame_nograv_bf = flame_balefire.clone().setGrav(0).setLife(100);

        flame_topaz_diesel =
                flame_diesel.clone().setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0.0D);
        flame_topaz_gas = flame_gas.clone().setProjectiles(2).setSpread(0.05F);
        flame_topaz_napalm =
                flame_napalm.clone().setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0.0D);
        flame_topaz_balefire =
                flame_balefire.clone().setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0.0D);

        flame_daybreaker_diesel =
                flame_diesel
                        .clone()
                        .setLife(200)
                        .setVel(2F)
                        .setGrav(0.035D)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 5F);
                                    spawnFire(
                                            bullet,
                                            mop,
                                            6F,
                                            2F,
                                            200,
                                            FireLingeringEntity.TYPE_DIESEL);
                                    bullet.discard();
                                });
        flame_daybreaker_gas =
                flame_gas
                        .clone()
                        .setLife(200)
                        .setVel(2F)
                        .setGrav(0.035D)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 5F);
                                    bullet.discard();
                                });
        flame_daybreaker_napalm =
                flame_napalm
                        .clone()
                        .setLife(200)
                        .setVel(2F)
                        .setGrav(0.035D)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 7.5F);
                                    spawnFire(
                                            bullet,
                                            mop,
                                            6F,
                                            2F,
                                            300,
                                            FireLingeringEntity.TYPE_DIESEL);
                                    bullet.discard();
                                });
        flame_daybreaker_balefire =
                flame_balefire
                        .clone()
                        .setLife(200)
                        .setVel(2F)
                        .setGrav(0.035D)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 5F);
                                    spawnFire(
                                            bullet,
                                            mop,
                                            7.5F,
                                            2.5F,
                                            400,
                                            FireLingeringEntity.TYPE_BALEFIRE);
                                    bullet.discard();
                                });

        ModItems.GUN_FLAMER =
                Registrar.registerItem(
                        "gun_flamer",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(20_000)
                                                        .draw(10)
                                                        .inspect(17)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(1F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(1)
                                                                        .auto(true)
                                                                        .reload(90)
                                                                        .jam(17)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                300)
                                                                                        .addConfigs(
                                                                                                flame_diesel,
                                                                                                flame_gas,
                                                                                                flame_napalm,
                                                                                                flame_balefire))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.25D)
                                                                        .setupStandardFire())
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_FLAMER_ANIMS)
                                                        .orchestra(GunAnimationActions.FLAMER))
                                        .setDefaultAmmo(EnumAmmo.FLAME_DIESEL, 1),
                        Item.Properties::new);
        ModItems.GUN_FLAMER_TOPAZ =
                Registrar.registerItem(
                        "gun_flamer_topaz",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.B_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(20_000)
                                                        .draw(10)
                                                        .inspect(17)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(1.5F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(1)
                                                                        .auto(true)
                                                                        .reload(90)
                                                                        .jam(17)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                500)
                                                                                        .addConfigs(
                                                                                                flame_topaz_diesel,
                                                                                                flame_topaz_gas,
                                                                                                flame_topaz_napalm,
                                                                                                flame_topaz_balefire))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.25D)
                                                                        .setupStandardFire())
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_FLAMER_ANIMS)
                                                        .orchestra(GunAnimationActions.FLAMER))
                                        .setDefaultAmmo(EnumAmmo.FLAME_DIESEL, 1),
                        Item.Properties::new);
        ModItems.GUN_FLAMER_DAYBREAKER =
                Registrar.registerItem(
                        "gun_flamer_daybreaker",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.LEGENDARY,
                                                props,
                                                GunConfig.builder()
                                                        .dura(20_000)
                                                        .draw(10)
                                                        .inspect(17)
                                                        .crosshair(Crosshair.L_CIRCLE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(25F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(10)
                                                                        .auto(true)
                                                                        .reload(90)
                                                                        .jam(17)
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
                                                                                                50)
                                                                                        .addConfigs(
                                                                                                flame_daybreaker_diesel,
                                                                                                flame_daybreaker_gas,
                                                                                                flame_daybreaker_napalm,
                                                                                                flame_daybreaker_balefire))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.25D)
                                                                        .setupStandardFire())
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_FLAMER_ANIMS)
                                                        .orchestra(
                                                                GunAnimationActions
                                                                        .FLAMER_DAYBREAKER))
                                        .setDefaultAmmo(EnumAmmo.FLAME_DIESEL, 1),
                        Item.Properties::new);

        ModItems.GUN_CHEMTHROWER =
                Registrar.registerItem(
                        "gun_chemthrower",
                        props ->
                                new GunChemthrowerItem(
                                        WeaponQuality.A_SIDE,
                                        props,
                                        GunConfig.builder()
                                                .dura(90_000)
                                                .draw(10)
                                                .inspect(17)
                                                .crosshair(Crosshair.L_CIRCLE)
                                                .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                .rec(
                                                        Receiver.builder()
                                                                .delay(1)
                                                                .spreadHipfire(0F)
                                                                .auto(true)
                                                                .mag(new MagazineFluid(0, 3_000))
                                                                .offset(0.75, -0.0625, -0.25D)
                                                                .canFire(
                                                                        GunChemthrowerItem
                                                                                .LAMBDA_CAN_FIRE)
                                                                .fire(
                                                                        GunChemthrowerItem
                                                                                .LAMBDA_FIRE))
                                                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                                                .anim(LAMBDA_CHEMTHROWER_ANIMS)
                                                .orchestra(GunAnimationActions.CHEMTHROWER)),
                        Item.Properties::new);
    }
}

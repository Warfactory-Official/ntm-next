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
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.FireLingeringEntity;
import com.hbm.entity.logic.C130Entity.C130PayloadType;
import com.hbm.entity.logic.C130Entity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.extprop.LivingHazards;
import com.hbm.items.ModItems;
import com.hbm.items.variant.CasingType;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.main.ResourceManager;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import com.hbm.platform.Platform;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class XFactory40mm {

    public static BulletConfig g26_flare;
    public static BulletConfig g26_flare_supply;
    public static BulletConfig g26_flare_weapon;

    public static BulletConfig g40_he;
    public static BulletConfig g40_heat;
    public static BulletConfig g40_demo;
    public static BulletConfig g40_inc;
    public static BulletConfig g40_phosphorus;

    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_IGNITE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    LivingHazards props = LivingHazards.getOrCreate(living);
                    props.fire += 200;
                }
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE =
            (bullet, mop) -> {
                Lego.standardExplode(bullet, mop, 5F);
                bullet.discard();
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE_HEAT =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Lego.standardExplode(bullet, mop, 3.5F);
                bullet.discard();
                if (mop instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    living.hurtServer(
                            (ServerLevel) living.level(),
                            ModifiedDamageSource.of(
                                    BulletConfig.getDamage(
                                            bullet, bullet.getThrower(), ModDamageTypes.EXPLOSIVE),
                                    true,
                                    0.5F,
                                    3F,
                                    0.15F),
                            bullet.damage * 3F);
                } else if (mop instanceof EntityHitResult entityHit) {
                    entityHit
                            .getEntity()
                            .hurtServer(
                                    (ServerLevel) bullet.level(),
                                    BulletConfig.getDamage(
                                            bullet, bullet.getThrower(), ModDamageTypes.EXPLOSIVE),
                                    bullet.damage * 3F);
                }
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE_DEMO =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Vec3 hit = mop.getLocation();
                ComposableExplosion vnt =
                        new ComposableExplosion(
                                bullet.level(), hit.x, hit.y, hit.z, 5F, bullet.getThrower());
                vnt.blockSelector(new AttenuatedRayBlockSelector());
                vnt.blockProcessor(CellDestruction.decay());
                vnt.entityProcessor(new WeaponExplosionEntityProcessor(1, bullet.damage));
                vnt.effects(new SmallExplosionEffect(10, 2.5F, 1F));
                vnt.explode();
                bullet.discard();
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE_INC =
            (bullet, mop) -> {
                spawnFire(bullet, mop, false, 200);
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE_PHOSPHORUS =
            (bullet, mop) -> {
                spawnFire(bullet, mop, true, 400);
            };
    public static Consumer<Entity> LAMBDA_SPAWN_C130_SUPPLIESS =
            (entity) -> {
                spawnPlane(entity, C130PayloadType.SUPPLIES);
            };
    public static Consumer<Entity> LAMBDA_SPAWN_C130_WEAPONS =
            (entity) -> {
                spawnPlane(entity, C130PayloadType.WEAPONS);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE =
            (stack, ctx) -> {
                Lego.handleStandardSmoke(ctx.entity(), stack, 1500, 0.025D, 1.05D, 0);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_GL =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MK108 =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        (float) (ctx.getPlayer().getRandom().nextGaussian()) + 1F,
                        (float) (ctx.getPlayer().getRandom().nextGaussian()));
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FLAREGUN_ANIMS =
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
                                                .to(15, 0, 0, 50)
                                                .to(15, 0, 0, 550)
                                                .to(0, 0, 0, 100));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(15, 0, 0, 50)
                                                .to(15, 0, 0, 550)
                                                .to(0, 0, 0, 100));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "OPEN",
                                        new AnimationSequence()
                                                .to(45, 0, 0, 200, IType.SIN_FULL)
                                                .to(45, 0, 0, 750)
                                                .to(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "SHELL",
                                        new AnimationSequence()
                                                .to(4, -8, -4, 0)
                                                .to(4, -8, -4, 200)
                                                .to(0, 0, -5, 500, IType.SIN_DOWN)
                                                .to(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "FLIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 200)
                                                .to(25, 0, 0, 200, IType.SIN_DOWN)
                                                .to(25, 0, 0, 800)
                                                .to(0, 0, 0, 200, IType.SIN_DOWN));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "OPEN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(45, 0, 0, 200, IType.SIN_FULL)
                                                .to(45, 0, 0, 500)
                                                .to(0, 0, 0, 200, IType.SIN_UP))
                                .addBus(
                                        "FLIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 500)
                                                .to(0, 0, 0, 200)
                                                .to(25, 0, 0, 200, IType.SIN_DOWN)
                                                .to(25, 0, 0, 550)
                                                .to(0, 0, 0, 200, IType.SIN_DOWN));
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "FLIP",
                                        new AnimationSequence()
                                                .to(-360 * 3, 0, 0, 1500, IType.SIN_FULL));
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CONGOLAKE_ANIMS =
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
                        return ResourceManager.congolake_anim.get("Equip");
                    case CYCLE:
                        return ResourceManager.congolake_anim.get(ammo <= 1 ? "FireEmpty" : "Fire");
                    case RELOAD:
                        return ResourceManager.congolake_anim.get(
                                ammo == 0 ? "ReloadEmpty" : "ReloadStart");
                    case RELOAD_CYCLE:
                        return ResourceManager.congolake_anim.get("Reload");
                    case RELOAD_END:
                        return ResourceManager.congolake_anim.get("ReloadEnd");
                    case JAMMED:
                        return ResourceManager.congolake_anim.get("Jammed");
                    case INSPECT:
                        return ResourceManager.congolake_anim.get("Inspect");
                }

                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MK108_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .jump(45, 0, 0)
                                                .to(0, 0, 0, 1000, IType.SIN_DOWN));
                    case CYCLE:
                        int amount =
                                ((GunBaseNTItem) stack.getItem())
                                        .getConfig(stack, 0)
                                        .getReceivers(stack)
                                        .get(0)
                                        .getMagazine(stack)
                                        .getAmount(stack, null);
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .stay(50)
                                                .to(0, 0, -0.25, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "BARREL",
                                        new AnimationSequence()
                                                .to(0, 0, -1, 100, IType.SIN_DOWN)
                                                .to(0, 0, 0, 250, IType.SIN_FULL))
                                .addBus(
                                        "CYCLE",
                                        new AnimationSequence().to(0, 0, 0, 100).to(1, 0, 0, 150))
                                .addBus("SHELLS", new AnimationSequence().jump(amount - 1, 0, 0));
                    case CYCLE_DRY:
                        return new BusAnimation()
                                .addBus(
                                        "HAMMER",
                                        new AnimationSequence()
                                                .to(15, 0, 0, 50)
                                                .to(15, 0, 0, 550)
                                                .to(0, 0, 0, 100));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .to(10, 0, 0, 500, IType.SIN_FULL)
                                                .stayUntil(1250)
                                                .to(-50, 0, 0, 750, IType.SIN_FULL)
                                                .stayUntil(5500)
                                                .to(0, 0, 0, 500, IType.SIN_FULL)
                                                .stay(500)
                                                .to(1, 0, 0, 100, IType.SIN_UP)
                                                .to(0, 0, 0, 150, IType.SIN_FULL))
                                .addBus(
                                        "LID",
                                        new AnimationSequence()
                                                .to(60, 0, 0, 500, IType.SIN_FULL)
                                                .stayUntil(6000)
                                                .to(0, 0, 0, 500, IType.SIN_UP))
                                .addBus(
                                        "BELT",
                                        new AnimationSequence()
                                                .jump(1, 0, 0)
                                                .stay(500)
                                                .to(0, 0, 0, 750, IType.SIN_UP)
                                                .stayUntil(4500)
                                                .to(1, 0, 0, 750, IType.SIN_UP))
                                .addBus(
                                        "DRUM",
                                        new AnimationSequence()
                                                .stay(2000)
                                                .to(2.5, 0, 0, 500, IType.SIN_DOWN)
                                                .to(2.5, -2, -8, 500, IType.SIN_UP)
                                                .jump(4, -3, -8)
                                                .to(2.5, 0, 0, 1000, IType.SIN_FULL)
                                                .to(0, 0, 0, 500, IType.SIN_UP));
                    case JAMMED:
                        return new BusAnimation()
                                .addBus(
                                        "LID",
                                        new AnimationSequence()
                                                .stay(250)
                                                .to(45, 0, 0, 500, IType.SIN_FULL)
                                                .to(0, 0, 0, 250, IType.SIN_UP))
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .stay(1000)
                                                .to(1, 0, 0, 100, IType.SIN_UP)
                                                .to(0, 0, 0, 150, IType.SIN_FULL));
                    case INSPECT:
                        int yeetHorizontal = 750;
                        int untilImpact = yeetHorizontal * 9 / 15;
                        int delay = 250;
                        int height = 6;
                        int arcUp = untilImpact * 5 / 8;
                        int arcDown = untilImpact * 3 / 8;
                        return new BusAnimation()
                                .addBus(
                                        "LIFT",
                                        new AnimationSequence()
                                                .stay(untilImpact)
                                                .to(1, 0, 0, 50, IType.SIN_UP)
                                                .to(0, 0, 0, 100, IType.SIN_FULL)
                                                .stay(delay - 150)
                                                .to(1, 0, 0, 50, IType.SIN_UP)
                                                .to(0, 0, 0, 100, IType.SIN_FULL)
                                                .stay(delay - 150)
                                                .to(1, 0, 0, 50, IType.SIN_UP)
                                                .to(0, 0, 0, 100, IType.SIN_FULL))
                                .addBus(
                                        "GRENH1",
                                        new AnimationSequence()
                                                .jump(9, 0, 0)
                                                .to(-6, 0, 0, yeetHorizontal))
                                .addBus(
                                        "GRENV1",
                                        new AnimationSequence()
                                                .jump(0, -2, 0)
                                                .to(0, height, 0, arcUp, IType.SIN_DOWN)
                                                .to(0, 2, 0, arcDown, IType.SIN_UP)
                                                .to(
                                                        0,
                                                        3,
                                                        0,
                                                        yeetHorizontal - untilImpact,
                                                        IType.SIN_DOWN))
                                .addBus(
                                        "GRENS1",
                                        new AnimationSequence()
                                                .to(360 * 2, 0, 0, untilImpact)
                                                .jump(0, 0, 0)
                                                .to(360, 0, 0, yeetHorizontal - untilImpact))
                                .addBus(
                                        "GRENH2",
                                        new AnimationSequence()
                                                .jump(9, 0, 0)
                                                .stay(delay)
                                                .to(-6, 0, 0, yeetHorizontal))
                                .addBus(
                                        "GRENV2",
                                        new AnimationSequence()
                                                .jump(0, -2, 0)
                                                .stay(delay)
                                                .to(0, height, 0, arcUp, IType.SIN_DOWN)
                                                .to(0, 2, 0, arcDown, IType.SIN_UP)
                                                .to(
                                                        0,
                                                        3,
                                                        0,
                                                        yeetHorizontal - untilImpact,
                                                        IType.SIN_DOWN))
                                .addBus(
                                        "GRENS2",
                                        new AnimationSequence()
                                                .stay(delay)
                                                .to(360 * 2, 0, 0, untilImpact)
                                                .jump(0, 0, 0)
                                                .to(360, 0, 0, yeetHorizontal - untilImpact))
                                .addBus(
                                        "GRENH3",
                                        new AnimationSequence()
                                                .jump(9, 0, 0)
                                                .stay(delay * 2)
                                                .to(-6, 0, 0, yeetHorizontal))
                                .addBus(
                                        "GRENV3",
                                        new AnimationSequence()
                                                .jump(0, -2, 0)
                                                .stay(delay * 2)
                                                .to(0, height, 0, arcUp, IType.SIN_DOWN)
                                                .to(0, 2, 0, arcDown, IType.SIN_UP)
                                                .to(
                                                        0,
                                                        3,
                                                        0,
                                                        yeetHorizontal - untilImpact,
                                                        IType.SIN_DOWN))
                                .addBus(
                                        "GRENS3",
                                        new AnimationSequence()
                                                .stay(delay * 2)
                                                .to(360 * 2, 0, 0, untilImpact)
                                                .jump(0, 0, 0)
                                                .to(360, 0, 0, yeetHorizontal - untilImpact));
                }
                return null;
            };

    public static void spawnFire(
            BulletBaseMK4Entity bullet, HitResult mop, boolean phosphorus, int duration) {
        if (mop instanceof EntityHitResult entityHit
                && bullet.tickCount < 3
                && entityHit.getEntity() == bullet.getThrower()) return;
        Level world = bullet.level();
        Lego.standardExplode(bullet, mop, 3F);
        FireLingeringEntity fire =
                new FireLingeringEntity(world)
                        .setArea(5, 2)
                        .setDuration(duration)
                        .setType(
                                phosphorus
                                        ? FireLingeringEntity.TYPE_PHOSPHORUS
                                        : FireLingeringEntity.TYPE_DIESEL);
        fire.setPos(mop.getLocation());
        world.addFreshEntity(fire);
        bullet.discard();
        Vec3 hit = mop.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = BlockPos.containing(hit.x, hit.y, hit.z).offset(dx, dy, dz);
                    if (world.getBlockState(pos).isAir())
                        for (Direction dir : Direction.VALUES) {
                            BlockPos neighbor = pos.relative(dir);
                            BlockState state = world.getBlockState(neighbor);
                            if (Platform.isFlammable(world, neighbor, state, dir.getOpposite())) {
                                world.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                                break;
                            }
                        }
                }
            }
        }
    }

    public static void spawnPlane(Entity entity, C130PayloadType payload) {
        if (!entity.level().isClientSide() && entity.tickCount == 40) {
            BulletBaseMK4Entity bullet = (BulletBaseMK4Entity) entity;
            if (bullet.getThrower() != null)
                WorldSounds.play(
                        entity.level(),
                        null,
                        bullet.getThrower(),
                        ModSounds.TECH_BLEEP.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
            C130Entity c130 = new C130Entity(ModEntities.C130.get(), bullet.level());
            int x = (int) Math.floor(bullet.getX());
            int z = (int) Math.floor(bullet.getZ());
            int y = bullet.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            c130.fac(bullet.level(), x, y, z, payload);
            bullet.level().addFreshEntity(c130);
        }
    }

    public static void init() {

        g26_flare =
                new BulletConfig()
                        .setItem(EnumAmmo.G26_FLARE)
                        .setCasing(CasingType.LARGE, 4)
                        .setLife(100)
                        .setVel(2F)
                        .setGrav(0.015D)
                        .setRenderRotations(false)
                        .setOnImpact(LAMBDA_STANDARD_IGNITE)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0x9E1616)
                                        .scale(2F)
                                        .register("g26Flare"));
        g26_flare_supply =
                new BulletConfig()
                        .setItem(EnumAmmo.G26_FLARE_SUPPLY)
                        .setCasing(CasingType.LARGE, 4)
                        .setLife(100)
                        .setVel(2F)
                        .setGrav(0.015D)
                        .setRenderRotations(false)
                        .setOnImpact(LAMBDA_STANDARD_IGNITE)
                        .setOnUpdate(LAMBDA_SPAWN_C130_SUPPLIESS)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0x3C80F0)
                                        .scale(2F)
                                        .register("g26FlareSupply"));
        g26_flare_weapon =
                new BulletConfig()
                        .setItem(EnumAmmo.G26_FLARE_WEAPON)
                        .setCasing(CasingType.LARGE, 4)
                        .setLife(100)
                        .setVel(2F)
                        .setGrav(0.015D)
                        .setRenderRotations(false)
                        .setOnImpact(LAMBDA_STANDARD_IGNITE)
                        .setOnUpdate(LAMBDA_SPAWN_C130_WEAPONS)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0x278400)
                                        .scale(2F)
                                        .register("g26FlareWeapon"));

        BulletConfig g40_base = new BulletConfig().setLife(200).setVel(2F).setGrav(0.035D);
        g40_he =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_HE)
                        .setCasing(CasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0x777777)
                                        .scale(2, 2F, 1.5F)
                                        .register("g40"));
        g40_heat =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_HEAT)
                        .setCasing(CasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_HEAT)
                        .setDamage(0.5F)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0x5E6854)
                                        .scale(2, 2F, 1.5F)
                                        .register("g40heat"));
        g40_demo =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_DEMO)
                        .setCasing(CasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_DEMO)
                        .setDamage(0.75F)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0xE30000)
                                        .scale(2, 2F, 1.5F)
                                        .register("g40demo"));
        g40_inc =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_INC)
                        .setCasing(CasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_INC)
                        .setDamage(0.75F)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0xE86F20)
                                        .scale(2, 2F, 1.5F)
                                        .register("g40inc"));
        g40_phosphorus =
                g40_base.clone()
                        .setItem(EnumAmmo.G40_PHOSPHORUS)
                        .setCasing(CasingType.LARGE, 4)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_PHOSPHORUS)
                        .setDamage(0.75F)
                        .setCasing(
                                Casing.template(CasingShape.STRAIGHT)
                                        .tints(0xC8C8C8)
                                        .scale(2, 2F, 1.5F)
                                        .register("g40phos"));

        ModItems.GUN_FLAREGUN =
                Registrar.registerItem(
                        "gun_flaregun",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(100)
                                                        .draw(7)
                                                        .inspect(39)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(15F)
                                                                        .delay(20)
                                                                        .reload(28)
                                                                        .jam(33)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_UNDERBARREL_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                1)
                                                                                        .addConfigs(
                                                                                                g26_flare,
                                                                                                g26_flare_supply,
                                                                                                g26_flare_weapon))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_GL))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_FLAREGUN_ANIMS)
                                                        .orchestra(GunAnimationActions.FLAREGUN))
                                        .setDefaultAmmo(EnumAmmo.G26_FLARE, 3),
                        Item.Properties::new);

        ModItems.GUN_CONGOLAKE =
                Registrar.registerItem(
                        "gun_congolake",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(400)
                                                        .draw(7)
                                                        .inspect(39)
                                                        .reloadSequential(true)
                                                        .reloadChangeType(true)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .smoke(LAMBDA_SMOKE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(20F)
                                                                        .delay(24)
                                                                        .reload(16, 16, 16, 0)
                                                                        .jam(0)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_CONGO_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                4)
                                                                                        .addConfigs(
                                                                                                g40_he,
                                                                                                g40_heat,
                                                                                                g40_demo,
                                                                                                g40_inc,
                                                                                                g40_phosphorus))
                                                                        .offset(
                                                                                0.75, -0.0625,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(LAMBDA_RECOIL_GL))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_CONGOLAKE_ANIMS)
                                                        .orchestra(GunAnimationActions.CONGOLAKE))
                                        .setDefaultAmmo(EnumAmmo.G40_HE, 8),
                        Item.Properties::new);

        ModItems.GUN_MK108 =
                Registrar.registerItem(
                        "gun_mk108",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(5_000)
                                                        .draw(20)
                                                        .inspect(65)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .hideCrosshair(false)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(25F)
                                                                        .delay(10)
                                                                        .auto(true)
                                                                        .dryfireAfterAuto(true)
                                                                        .reload(135)
                                                                        .jam(25)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_MK108_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                30)
                                                                                        .addConfigs(
                                                                                                g40_he,
                                                                                                g40_heat,
                                                                                                g40_demo,
                                                                                                g40_inc,
                                                                                                g40_phosphorus))
                                                                        .offset(
                                                                                0.75, -0.125,
                                                                                -0.125)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_MK108))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_MK108_ANIMS)
                                                        .orchestra(GunAnimationActions.MK108))
                                        .setDefaultAmmo(EnumAmmo.G40_HE, 50),
                        Item.Properties::new);
    }
}

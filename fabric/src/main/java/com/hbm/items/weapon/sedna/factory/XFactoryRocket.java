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
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.impl.GunStingerItem;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.platform.Platform;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class XFactoryRocket {

    public static BulletConfig[] rocket_template;

    public static BulletConfig[] rocket_rpzb;
    public static BulletConfig[] rocket_qd;
    public static BulletConfig[] rocket_ml;
    public static BulletConfig[] rocket_ncrpa;
    public static BulletConfig[] rocket_ncrpa_steer;

    public static Consumer<Entity> LAMBDA_STANDARD_ACCELERATE =
            (entity) -> {
                BulletBaseMK4Entity bullet = (BulletBaseMK4Entity) entity;
                if (bullet.accel < 7) bullet.setAcceleration(bullet.accel + 0.4D);
            };
    public static Consumer<Entity> LAMBDA_STEERING_ACCELERATE =
            (entity) -> {
                BulletBaseMK4Entity bullet = (BulletBaseMK4Entity) entity;
                if (!(bullet.getThrower() instanceof Player player)) {
                    if (bullet.accel < 7) bullet.setAcceleration(bullet.accel + 0.4D);
                    return;
                }
                ItemStack held = player.getMainHandItem();
                steeringAccelerate(
                        entity,
                        held.isEmpty()
                                || !(held.getItem() instanceof GunBaseNTItem)
                                || !GunBaseNTItem.getIsAiming(held));
            };
    public static Consumer<Entity> LAMBDA_NCR_ACCELERATE =
            (entity) -> {
                steeringAccelerate(entity, false);
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult && bullet.tickCount < 3) return;
                Lego.standardExplode(bullet, mop, 5F);
                bullet.discard();
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE_HEAT =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult && bullet.tickCount < 3) return;
                Lego.standardExplode(bullet, mop, 3.5F);
                bullet.discard();
                if (mop instanceof EntityHitResult entityHit) {
                    if (entityHit.getEntity() instanceof LivingEntity living) {
                        living.hurtServer(
                                (ServerLevel) living.level(),
                                ModifiedDamageSource.of(
                                        BulletConfig.getDamage(
                                                bullet,
                                                bullet.getThrower(),
                                                ModDamageTypes.EXPLOSIVE),
                                        true,
                                        0.5F,
                                        5F,
                                        0.2F),
                                bullet.damage * 3F);
                    } else {
                        entityHit
                                .getEntity()
                                .hurtServer(
                                        (ServerLevel) entityHit.getEntity().level(),
                                        ModifiedDamageSource.bypassingCooldown(
                                                BulletConfig.getDamage(
                                                        bullet,
                                                        bullet.getThrower(),
                                                        ModDamageTypes.EXPLOSIVE)),
                                        bullet.damage * 3F);
                    }
                }
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE_DEMO =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult && bullet.tickCount < 3) return;
                ComposableExplosion vnt =
                        new ComposableExplosion(
                                bullet.level(),
                                mop.getLocation().x,
                                mop.getLocation().y,
                                mop.getLocation().z,
                                5F,
                                bullet.getThrower());
                vnt.blockSelector(new AttenuatedRayBlockSelector());
                vnt.blockProcessor(CellDestruction.decay());
                vnt.entityProcessor(new WeaponExplosionEntityProcessor(1, bullet.damage));
                vnt.effects(new SmallExplosionEffect(10, 2.5F, 1F));
                vnt.explode();
                bullet.discard();
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE_INC =
            (bullet, mop) -> {
                spawnFire(bullet, mop, false, 300);
            };
    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_STANDARD_EXPLODE_PHOSPHORUS =
            (bullet, mop) -> {
                spawnFire(bullet, mop, true, 600);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STINGER_SECONDARY_PRESS =
            (stack, ctx) -> {
                GunStingerItem.setIsLockingOn(stack, true);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STINGER_SECONDARY_RELEASE =
            (stack, ctx) -> {
                GunStingerItem.setIsLockingOn(stack, false);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_MISSILE_LAUNCHER_PRIMARY_PRESS =
            (stack, ctx) -> {
                if (GunBaseNTItem.getIsAiming(stack)) {
                    int target = GunStingerItem.getLockonTarget(ctx.getPlayer(), 150D, 20D);
                    if (target != -1) {
                        GunBaseNTItem.setLockonTarget(stack, target);
                        GunBaseNTItem.setIsLockedOn(stack, true);
                    }
                }
                Lego.LAMBDA_STANDARD_CLICK_PRIMARY.accept(stack, ctx);
                GunBaseNTItem.setIsLockedOn(stack, false);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ROCKET = (stack, ctx) -> {};

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_PANZERSCHRECK_ANIMS =
            (stack, type) -> {
                boolean empty =
                        ((GunBaseNTItem) stack.getItem())
                                        .getConfig(stack, 0)
                                        .getReceivers(stack)
                                        .get(0)
                                        .getMagazine(stack)
                                        .getAmount(
                                                stack, ClientPlayerAccess.player().getInventory())
                                <= 0;
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(60, 0, 0, 0)
                                                .to(0, 0, 0, 500, IType.SIN_DOWN));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "RELOAD",
                                        new AnimationSequence()
                                                .to(90, 0, 0, 750, IType.SIN_FULL)
                                                .to(90, 0, 0, 1000)
                                                .to(0, 0, 0, 750, IType.SIN_FULL))
                                .addBus(
                                        "ROCKET",
                                        new AnimationSequence()
                                                .to(0, -3, -6, 0)
                                                .to(0, -3, -6, 750)
                                                .to(0, 0, -6.5, 500, IType.SIN_DOWN)
                                                .to(0, 0, 0, 350, IType.SIN_UP));
                    case JAMMED:
                        empty = false;
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "RELOAD",
                                        new AnimationSequence()
                                                .to(90, 0, 0, 750, IType.SIN_FULL)
                                                .to(90, 0, 0, 500)
                                                .to(0, 0, 0, 750, IType.SIN_FULL))
                                .addBus(
                                        "ROCKET",
                                        new AnimationSequence().to(0, empty ? -3 : 0, 0, 0));
                }
                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_QUADRO_ANIMS =
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
                                        new AnimationSequence().to(0, 0, -0.5, 50).to(0, 0, 0, 50));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "RELOAD_ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, 60, 500, IType.SIN_FULL)
                                                .to(0, 0, 60, 1500)
                                                .to(0, 0, 0, 750, IType.SIN_FULL))
                                .addBus(
                                        "RELOAD_PUSH",
                                        new AnimationSequence()
                                                .to(-1, -1, 0, 0)
                                                .to(-1, -1, 0, 500)
                                                .to(-1, 0, 0, 350)
                                                .to(0, 0, 0, 1000));
                    case JAMMED:
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "RELOAD_ROTATE",
                                        new AnimationSequence()
                                                .to(0, 0, 60, 750, IType.SIN_FULL)
                                                .to(0, 0, 60, 500)
                                                .to(0, 0, 0, 750, IType.SIN_FULL));
                }
                return null;
            };

    @SuppressWarnings("incomplete-switch")
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MISSILE_LAUNCHER_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(60, 0, 0, 0)
                                                .to(0, 0, 0, 1000, IType.SIN_DOWN));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "BARREL",
                                        new AnimationSequence()
                                                .to(0, 0, 1.5, 150)
                                                .to(0, 0, 1.5, 2100)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "OPEN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(90, 0, 0, 500, IType.SIN_FULL)
                                                .to(90, 0, 0, 1000)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 2250)
                                                .to(-1, 0, 0, 150, IType.SIN_DOWN)
                                                .to(0, 0, 0, 150, IType.SIN_UP))
                                .addBus(
                                        "MISSILE",
                                        new AnimationSequence()
                                                .to(-10, 0, 0, 0)
                                                .to(-10, 0, 0, 750)
                                                .to(3, 0, 2, 0)
                                                .to(0, 0, -6, 350, IType.SIN_FULL)
                                                .to(0, 0, 0, 350, IType.SIN_UP));
                    case JAMMED:
                    case INSPECT:
                        return new BusAnimation()
                                .addBus(
                                        "BARREL",
                                        new AnimationSequence()
                                                .to(0, 0, 1.5, 150)
                                                .to(0, 0, 1.5, 1350)
                                                .to(0, 0, 0, 150))
                                .addBus(
                                        "OPEN",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 250)
                                                .to(90, 0, 0, 500, IType.SIN_FULL)
                                                .to(90, 0, 0, 250)
                                                .to(0, 0, 0, 500, IType.SIN_FULL))
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1500)
                                                .to(-1, 0, 0, 150, IType.SIN_DOWN)
                                                .to(0, 0, 0, 150, IType.SIN_UP));
                }
                return null;
            };

    public static void steeringAccelerate(Entity entity, boolean noSteer) {
        BulletBaseMK4Entity bullet = (BulletBaseMK4Entity) entity;
        if (bullet.accel < 4) bullet.setAcceleration(bullet.accel + 0.4D);
        if (!(bullet.getThrower() instanceof Player player)) return;

        if (new Vec3(
                                bullet.getX() - player.getX(),
                                bullet.getY() - player.getY(),
                                bullet.getZ() - player.getZ())
                        .length()
                > 100) return;
        if (noSteer) return;

        HitResult mop = player.pick(200, 1F, false);
        if (mop == null) return;

        Vec3 vec =
                new Vec3(
                        mop.getLocation().x - bullet.getX(),
                        mop.getLocation().y - bullet.getY(),
                        mop.getLocation().z - bullet.getZ());
        if (vec.length() < 3) return;
        vec = vec.normalize();

        double speed = bullet.getFlightDirection().length();
        bullet.setFlightDirection(vec.x * speed, vec.y * speed, vec.z * speed);
    }

    public static void spawnFire(
            BulletBaseMK4Entity bullet, HitResult mop, boolean phosphorus, int duration) {
        if (mop instanceof EntityHitResult && bullet.tickCount < 3) return;
        Level world = bullet.level();
        Lego.standardExplode(bullet, mop, 3F);
        FireLingeringEntity fire =
                new FireLingeringEntity(world)
                        .setArea(6, 2)
                        .setDuration(duration)
                        .setType(
                                phosphorus
                                        ? FireLingeringEntity.TYPE_PHOSPHORUS
                                        : FireLingeringEntity.TYPE_DIESEL);
        fire.setPos(mop.getLocation());
        world.addFreshEntity(fire);
        bullet.discard();
        Vec3 hit = mop.getLocation();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
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

    public static BulletConfig makeRPZB(BulletConfig original) {
        return original.clone();
    }

    public static BulletConfig makeQD(BulletConfig original) {
        return original.clone().setLife(400).setOnUpdate(LAMBDA_STEERING_ACCELERATE);
    }

    public static BulletConfig makeNCR(BulletConfig original) {
        return original.clone().setLife(400).setOnUpdate(LAMBDA_NCR_ACCELERATE);
    }

    public static BulletConfig makeML(BulletConfig original) {
        return original.clone();
    }

    public static void init() {

        rocket_template = new BulletConfig[5];

        BulletConfig baseRocket =
                new BulletConfig()
                        .setLife(300)
                        .setSelfDamageDelay(10)
                        .setVel(0F)
                        .setGrav(0D)
                        .setOnEntityHit(null)
                        .setOnRicochet(null)
                        .setOnUpdate(LAMBDA_STANDARD_ACCELERATE);

        rocket_template[0] =
                baseRocket.clone().setItem(EnumAmmo.ROCKET_HE).setOnImpact(LAMBDA_STANDARD_EXPLODE);
        rocket_template[1] =
                baseRocket
                        .clone()
                        .setItem(EnumAmmo.ROCKET_HEAT)
                        .setDamage(0.5F)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_HEAT);
        rocket_template[2] =
                baseRocket
                        .clone()
                        .setItem(EnumAmmo.ROCKET_DEMO)
                        .setDamage(0.75F)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_DEMO);
        rocket_template[3] =
                baseRocket
                        .clone()
                        .setItem(EnumAmmo.ROCKET_INC)
                        .setDamage(0.75F)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_INC);
        rocket_template[4] =
                baseRocket
                        .clone()
                        .setItem(EnumAmmo.ROCKET_PHOSPHORUS)
                        .setDamage(0.75F)
                        .setOnImpact(LAMBDA_STANDARD_EXPLODE_PHOSPHORUS);

        rocket_rpzb = new BulletConfig[rocket_template.length];
        rocket_qd = new BulletConfig[rocket_template.length];
        rocket_ml = new BulletConfig[rocket_template.length];
        rocket_ncrpa_steer = new BulletConfig[rocket_template.length];
        rocket_ncrpa = new BulletConfig[rocket_template.length];

        for (int i = 0; i < rocket_template.length; i++) {
            rocket_rpzb[i] = makeRPZB(rocket_template[i]);
            rocket_qd[i] = makeQD(rocket_template[i]);
            rocket_ml[i] = makeML(rocket_template[i]);
            rocket_ncrpa_steer[i] = makeNCR(rocket_template[i]);
            rocket_ncrpa[i] = makeRPZB(rocket_template[i]);
        }

        ModItems.GUN_PANZERSCHRECK =
                Registrar.registerItem(
                        "gun_panzerschreck",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(300)
                                                        .draw(7)
                                                        .inspect(40)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(25F)
                                                                        .delay(5)
                                                                        .reload(50)
                                                                        .jam(40)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_ROCKET_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                1)
                                                                                        .addConfigs(
                                                                                                rocket_rpzb))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ROCKET))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_PANZERSCHRECK_ANIMS)
                                                        .orchestra(GunAnimationActions.LAUNCHER))
                                        .setDefaultAmmo(EnumAmmo.ROCKET_HE, 3),
                        Item.Properties::new);

        ModItems.GUN_STINGER =
                Registrar.registerItem(
                        "gun_stinger",
                        props ->
                                new GunStingerItem(
                                                WeaponQuality.SPECIAL,
                                                props,
                                                GunConfig.builder()
                                                        .dura(300)
                                                        .draw(7)
                                                        .inspect(40)
                                                        .crosshair(Crosshair.L_BOX_OUTLINE)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(35F)
                                                                        .delay(5)
                                                                        .reload(50)
                                                                        .jam(40)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_ROCKET_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                1)
                                                                                        .addConfigs(
                                                                                                rocket_rpzb))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875D)
                                                                        .setupLockonFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ROCKET))
                                                        .setupStandardConfiguration()
                                                        .ps(LAMBDA_STINGER_SECONDARY_PRESS)
                                                        .rs(LAMBDA_STINGER_SECONDARY_RELEASE)
                                                        .anim(LAMBDA_PANZERSCHRECK_ANIMS)
                                                        .orchestra(GunAnimationActions.STINGER))
                                        .setDefaultAmmo(EnumAmmo.ROCKET_HEAT, 3),
                        Item.Properties::new);

        ModItems.GUN_QUADRO =
                Registrar.registerItem(
                        "gun_quadro",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(400)
                                                        .draw(7)
                                                        .inspect(40)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .hideCrosshair(false)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(40F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(10)
                                                                        .reload(55)
                                                                        .jam(40)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_ROCKET_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineFullReload(
                                                                                                0,
                                                                                                4)
                                                                                        .addConfigs(
                                                                                                rocket_qd))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ROCKET))
                                                        .setupStandardConfiguration()
                                                        .anim(LAMBDA_QUADRO_ANIMS)
                                                        .orchestra(GunAnimationActions.LAUNCHER))
                                        .setDefaultAmmo(EnumAmmo.ROCKET_HE, 4),
                        Item.Properties::new);

        ModItems.GUN_MISSILE_LAUNCHER =
                Registrar.registerItem(
                        "gun_missile_launcher",
                        props ->
                                new GunBaseNTItem(
                                                WeaponQuality.A_SIDE,
                                                props,
                                                GunConfig.builder()
                                                        .dura(500)
                                                        .draw(20)
                                                        .inspect(40)
                                                        .crosshair(Crosshair.L_CIRCUMFLEX)
                                                        .hideCrosshair(false)
                                                        .rec(
                                                                Receiver.builder()
                                                                        .dmg(50F)
                                                                        .spreadHipfire(0F)
                                                                        .delay(5)
                                                                        .reload(48)
                                                                        .jam(33)
                                                                        .sound(
                                                                                () ->
                                                                                        ModSounds
                                                                                                .GUN_ROCKET_FIRE
                                                                                                .get(),
                                                                                1.0F,
                                                                                1.0F)
                                                                        .mag(
                                                                                new MagazineSingleReload(
                                                                                                0,
                                                                                                1)
                                                                                        .addConfigs(
                                                                                                rocket_ml))
                                                                        .offset(
                                                                                1,
                                                                                -0.0625 * 1.5,
                                                                                -0.1875D)
                                                                        .setupStandardFire()
                                                                        .recoil(
                                                                                LAMBDA_RECOIL_ROCKET))
                                                        .setupStandardConfiguration()
                                                        .pp(LAMBDA_MISSILE_LAUNCHER_PRIMARY_PRESS)
                                                        .anim(LAMBDA_MISSILE_LAUNCHER_ANIMS)
                                                        .orchestra(
                                                                GunAnimationActions
                                                                        .MISSILE_LAUNCHER))
                                        .setDefaultAmmo(EnumAmmo.ROCKET_HEAT, 5),
                        Item.Properties::new);
    }
}

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

import com.hbm.entity.effect.NukeTorexEntity;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.entity.projectile.BulletBeamBaseEntity;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlasmaBlastLayout;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class XFactoryFolly {

    public static BulletConfig folly_sm;
    public static BulletConfig folly_nuke;

    public static Consumer<Entity> LAMBDA_SM_UPDATE =
            (entity) -> {
                if (!(entity.level() instanceof ServerLevel server)) return;
                BulletBeamBaseEntity beam = (BulletBeamBaseEntity) entity;
                Vec3 dir = new Vec3(beam.headingX, beam.headingY, beam.headingZ).normalize();

                if (beam.tickCount < 50) {
                    double spacing = 10;
                    double dist = beam.tickCount * spacing;
                    PacketWire.sendToAllAround(
                            PlasmaBlastLayout.encode(
                                    beam.getX() + dir.x * dist,
                                    beam.getY() + dir.y * dist,
                                    beam.getZ() + dir.z * dist,
                                    0.75F,
                                    0.75F,
                                    0.75F,
                                    beam.getXRot() + 90,
                                    -beam.getYRot(),
                                    2F
                                            + beam.tickCount
                                                    / (float) (beam.getBeamLength() / spacing)
                                                    * 3F),
                            new TargetPoint(server, beam.getX(), beam.getY(), beam.getZ(), 250));
                }

                if (beam.tickCount != 2) return;

                if (beam.getThrower() != null)
                    ContaminationUtil.contaminate(
                            beam.getThrower(),
                            HazardType.RADIATION,
                            ContaminationType.CREATIVE,
                            150F);

                List<Entity> entities =
                        server.getEntities(
                                beam,
                                beam.getBoundingBox()
                                        .expandTowards(beam.headingX, beam.headingY, beam.headingZ)
                                        .inflate(1.0D, 1.0D, 1.0D));

                for (int i = 1; i < beam.getBeamLength(); i += 2) {
                    int x = (int) Math.floor(beam.getX() + dir.x * i);
                    int y = (int) Math.floor(beam.getY() + dir.y * i);
                    int z = (int) Math.floor(beam.getZ() + dir.z * i);

                    for (int ix = x - 1; ix <= x + 1; ix++)
                        for (int iy = y - 1; iy <= y + 1; iy++)
                            for (int iz = z - 1; iz <= z + 1; iz++) {
                                if (iy > server.getMinY() && iy <= server.getMaxY())
                                    server.setBlockAndUpdate(
                                            new BlockPos(ix, iy, iz),
                                            Blocks.AIR.defaultBlockState());
                                AABB aabb =
                                        new AABB(ix - 1, iy - 1, iz - 1, ix + 2, iy + 2, iz + 2);
                                for (Entity e : entities)
                                    if (e != beam.getThrower()
                                            && e.getBoundingBox().intersects(aabb)) {
                                        if (e instanceof LivingEntity living)
                                            living.hurtServer(
                                                    server,
                                                    ModifiedDamageSource.of(
                                                            BulletConfig.getDamage(
                                                                    beam,
                                                                    beam.getThrower(),
                                                                    beam.config.dmgClass),
                                                            true,
                                                            0D,
                                                            100F,
                                                            0.99F),
                                                    beam.damage);
                                        else
                                            e.hurtServer(
                                                    server,
                                                    ModifiedDamageSource.bypassingCooldown(
                                                            BulletConfig.getDamage(
                                                                    beam,
                                                                    beam.getThrower(),
                                                                    beam.config.dmgClass)),
                                                    beam.damage);
                                    }
                            }
                }
            };

    public static BiConsumer<BulletBaseMK4Entity, HitResult> LAMBDA_NUKE_IMPACT =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult && bullet.tickCount < 2) return;
                if (bullet.isRemoved()) return;
                bullet.discard();
                Vec3 hit = mop.getLocation();
                bullet.level()
                        .addFreshEntity(
                                NukeExplosionMK5Entity.statFac(
                                        bullet.level(), 100, hit.x, hit.y, hit.z));
                NukeTorexEntity.statFac(bullet.level(), hit.x, hit.y, hit.z, 100);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TOGGLE_AIM =
            (stack, ctx) -> {
                if (GunBaseNTItem.getState(stack, ctx.configIndex()) == GunState.IDLE) {
                    boolean wasAiming = GunBaseNTItem.getIsAiming(stack);
                    GunBaseNTItem.setIsAiming(stack, !wasAiming);
                    if (!wasAiming)
                        GunBaseNTItem.playAnimation(
                                ctx.getPlayer(), stack, GunAnimation.SPINUP, ctx.configIndex());
                }
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE =
            (stack, ctx) -> {
                Lego.doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);
            };
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_CAN_FIRE =
            (stack, ctx) -> {
                if (ctx.entity() instanceof Player) {
                    if (!GunBaseNTItem.getIsAiming(stack)) return false;
                    if (GunBaseNTItem.getLastAnim(stack, ctx.configIndex()) != GunAnimation.SPINUP)
                        return false;
                    if (GunBaseNTItem.getAnimTimer(stack, ctx.configIndex()) < 100) return false;
                }
                return ctx.config()
                                .getReceivers(stack)
                                .get(0)
                                .getMagazine(stack)
                                .getAmount(stack, ctx.inventory())
                        > 0;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_FOLLY =
            (stack, ctx) -> {
                GunBaseNTItem.setupRecoil(
                        25, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
            };
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FOLLY_ANIMS =
            (stack, type) -> {
                switch (type) {
                    case EQUIP:
                        return new BusAnimation()
                                .addBus(
                                        "EQUIP",
                                        new AnimationSequence()
                                                .to(-60, 0, 0, 0)
                                                .to(5, 0, 0, 1500, IType.SIN_DOWN)
                                                .to(0, 0, 0, 500, IType.SIN_FULL));
                    case CYCLE:
                        return new BusAnimation()
                                .addBus(
                                        "RECOIL",
                                        new AnimationSequence()
                                                .to(0, 0, -4.5, 50)
                                                .to(0, 0, -4.5, 500)
                                                .to(0, 0, 0, 500, IType.SIN_UP))
                                .addBus(
                                        "LOAD",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 50)
                                                .to(-25, 0, 0, 250, IType.SIN_DOWN)
                                                .to(0, 0, 0, 1000, IType.SIN_FULL));
                    case RELOAD:
                        return new BusAnimation()
                                .addBus(
                                        "LOAD",
                                        new AnimationSequence()
                                                .to(60, 0, 0, 1000, IType.SIN_FULL)
                                                .to(60, 0, 0, 6000)
                                                .to(0, 0, 0, 1000, IType.SIN_FULL))
                                .addBus(
                                        "SCREW",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1000)
                                                .to(0, 0, -135, 1000, IType.SIN_FULL)
                                                .to(0, 0, -135, 4000)
                                                .to(0, 0, 0, 1000, IType.SIN_FULL))
                                .addBus(
                                        "BREECH",
                                        new AnimationSequence()
                                                .to(0, 0, 0, 1000)
                                                .to(0, 0, -0.5, 1000, IType.SIN_FULL)
                                                .to(0, -4, -0.5, 1000, IType.SIN_FULL)
                                                .to(0, -4, -0.5, 2000)
                                                .to(0, 0, -0.5, 1000, IType.SIN_FULL)
                                                .to(0, 0, 0, 1000, IType.SIN_FULL))
                                .addBus(
                                        "SHELL",
                                        new AnimationSequence()
                                                .to(0, -4, -4.5, 0)
                                                .to(0, -4, -4.5, 3000)
                                                .to(0, 0, -4.5, 1000, IType.SIN_FULL)
                                                .to(0, 0, 0, 500, IType.SIN_UP));
                    default:
                        return null;
                }
            };

    public static void init() {

        folly_sm =
                new BulletConfig()
                        .setItem(EnumAmmoSecret.FOLLY_SM)
                        .setupDamageClass(ModDamageTypes.SUBATOMIC)
                        .setBeam()
                        .setLife(100)
                        .setVel(2F)
                        .setGrav(0.015D)
                        .setRenderRotations(false)
                        .setSpectral(true)
                        .setDoesPenetrate(true)
                        .setOnUpdate(LAMBDA_SM_UPDATE);
        folly_nuke =
                new BulletConfig()
                        .setItem(EnumAmmoSecret.FOLLY_NUKE)
                        .setChunkloading()
                        .setLife(600)
                        .setVel(4F)
                        .setGrav(0.015D)
                        .setOnImpact(LAMBDA_NUKE_IMPACT);

        ModItems.GUN_FOLLY =
                Registrar.registerItem(
                        "gun_folly",
                        props ->
                                new GunBaseNTItem(
                                        WeaponQuality.SECRET,
                                        props,
                                        GunConfig.builder()
                                                .dura(0)
                                                .draw(40)
                                                .crosshair(Crosshair.NONE)
                                                .rec(
                                                        Receiver.builder()
                                                                .dmg(1_000F)
                                                                .delay(26)
                                                                .dryfire(false)
                                                                .reload(160)
                                                                .jam(0)
                                                                .sound(
                                                                        () ->
                                                                                ModSounds
                                                                                        .GUN_PLEASE_REMOVE_MY_EARDRUMS_THANKS
                                                                                        .get(),
                                                                        100.0F,
                                                                        1.0F)
                                                                .mag(
                                                                        new MagazineSingleReload(
                                                                                        0, 1)
                                                                                .addConfigs(
                                                                                        folly_sm,
                                                                                        folly_nuke))
                                                                .offset(0.75, -0.0625, -0.1875D)
                                                                .offsetScoped(
                                                                        0.75, -0.0625, -0.125D)
                                                                .canFire(LAMBDA_CAN_FIRE)
                                                                .fire(LAMBDA_FIRE)
                                                                .recoil(LAMBDA_RECOIL_FOLLY))
                                                .setupStandardConfiguration()
                                                .pt(LAMBDA_TOGGLE_AIM)
                                                .anim(LAMBDA_FOLLY_ANIMS)
                                                .orchestra(GunAnimationActions.FOLLY)),
                        Item.Properties::new);
    }
}

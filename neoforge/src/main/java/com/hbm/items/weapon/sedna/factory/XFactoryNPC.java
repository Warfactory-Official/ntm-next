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
import com.hbm.explosion.NuclearShockwave;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.NtmMath;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class XFactoryNPC {
    public static final float MASKMAN_ORB_DAMAGE = 100F;
    public static final float MASKMAN_BOLT_DAMAGE = 17.5F;
    public static final float MASKMAN_TRACER_DAMAGE = 17.5F;
    public static final float MASKMAN_ROCKET_DAMAGE = 17.5F;
    public static final float MASKMAN_METEOR_DAMAGE = 25F;
    public static final float WORM_BOLT_DAMAGE = 20F;
    public static final float WORM_LASER_DAMAGE = 47.5F;
    public static final float UFO_ROCKET_DAMAGE = 12.5F;

    public static BulletConfig maskman_orb;
    public static BulletConfig maskman_bolt;
    public static BulletConfig maskman_tracer;
    public static BulletConfig maskman_meteor;
    public static BulletConfig maskman_rocket;
    public static BulletConfig worm_bolt;
    public static BulletConfig worm_laser;
    public static BulletConfig ufo_rocket;

    private XFactoryNPC() {}

    public static void init() {
        maskman_bolt =
                new BulletConfig()
                        .setVel(0.5F)
                        .setSpread(0F)
                        .setGrav(0D)
                        .setLife(100)
                        .setDoesPenetrate(true)
                        .setupDamageClass(ModDamageTypes.LASER)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 0.5F);
                                    bullet.discard();
                                });

        maskman_meteor =
                new BulletConfig()
                        .setVel(1F)
                        .setSpread(0F)
                        .setGrav(0.1D)
                        .setLife(300)
                        .setRicochetCount(0)
                        .setOnRicochet(null)
                        .setupDamageClass(ModDamageTypes.FIRE)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 2.5F);
                                    Vec3 hit = mop.getLocation();
                                    for (Entity entity :
                                            bullet.level()
                                                    .getEntities(
                                                            bullet,
                                                            bullet.getBoundingBox()
                                                                    .inflate(2.5D))) {
                                        if (entity.distanceToSqr(hit) < 2.5D * 2.5D) {
                                            entity.igniteForSeconds(3);
                                        }
                                    }
                                    bullet.discard();
                                });

        maskman_tracer =
                new BulletConfig()
                        .setVel(1F)
                        .setSpread(0F)
                        .setGrav(0D)
                        .setLife(100)
                        .setDoesPenetrate(true)
                        .setupDamageClass(ModDamageTypes.LASER)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    summonMeteor(bullet);
                                    bullet.discard();
                                });

        maskman_rocket =
                new BulletConfig()
                        .setVel(1F)
                        .setSpread(0F)
                        .setGrav(0.1D)
                        .setLife(300)
                        .setRicochetCount(0)
                        .setOnRicochet(null)
                        .setupDamageClass(ModDamageTypes.EXPLOSIVE)
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 5F);
                                    bullet.discard();
                                });

        maskman_orb =
                new BulletConfig()
                        .setVel(2F)
                        .setSpread(0F)
                        .setGrav(0D)
                        .setLife(60)
                        .setRicochetCount(0)
                        .setOnRicochet(null)
                        .setDoesPenetrate(false)
                        .setupDamageClass(ModDamageTypes.LASER)
                        .setOnUpdate(
                                entity -> {
                                    if (entity.level().isClientSide()) return;
                                    if (entity.tickCount % 10 != 5) return;
                                    rainBolts((BulletBaseMK4Entity) entity);
                                })
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 1.5F);
                                    bullet.discard();
                                });

        worm_bolt =
                new BulletConfig()
                        .setVel(0.5F)
                        .setSpread(0F)
                        .setGrav(0D)
                        .setLife(60)
                        .setRicochetCount(0)
                        .setOnRicochet(null)
                        .setDoesPenetrate(true)
                        .setupDamageClass(ModDamageTypes.LASER);

        worm_laser =
                new BulletConfig()
                        .setVel(1F)
                        .setSpread(0F)
                        .setGrav(0D)
                        .setLife(100)
                        .setRicochetCount(0)
                        .setOnRicochet(null)
                        .setDoesPenetrate(true)
                        .setupDamageClass(ModDamageTypes.LASER);

        ufo_rocket =
                new BulletConfig()
                        .setVel(2F)
                        .setSpread(0.005F)
                        .setGrav(0.005D)
                        .setLife(300)
                        .setRicochetAngle(10F)
                        .setRicochetCount(2)
                        .setupDamageClass(ModDamageTypes.EXPLOSIVE)
                        .setOnUpdate(
                                entity -> {
                                    if (entity.level().isClientSide()) return;
                                    BulletBaseMK4Entity bullet = (BulletBaseMK4Entity) entity;
                                    if (bullet.lockonTarget == null
                                            || !bullet.lockonTarget.isAlive()
                                            || bullet.lockonTarget.isSpectator()) {
                                        bullet.lockonTarget = chooseTarget(bullet);
                                    }
                                })
                        .setOnImpact(
                                (bullet, mop) -> {
                                    WorldSounds.play(
                                            bullet.level(),
                                            null,
                                            bullet.getX(),
                                            bullet.getY(),
                                            bullet.getZ(),
                                            ModSounds.UFO_BLAST.get(),
                                            SoundSource.HOSTILE,
                                            5F,
                                            0.9F + bullet.getRandom().nextFloat() * 0.2F);
                                    NuclearShockwave.sweep(
                                            bullet.level(),
                                            bullet.getX(),
                                            bullet.getY(),
                                            bullet.getZ(),
                                            10,
                                            50F);
                                    bullet.discard();
                                });
    }

    public static @Nullable BulletBaseMK4Entity aimed(
            LivingEntity shooter, BulletConfig config, float damage, Entity target, float spread) {

        double posY = shooter.getEyeY() - 0.10000000149011612D;
        double dx = target.getX() - shooter.getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 3F - posY;
        double dz = target.getZ() - shooter.getZ();
        double hyp = Math.sqrt(dx * dx + dz * dz);

        if (hyp < 1.0E-7D) return null;

        return new BulletBaseMK4Entity(
                shooter.level(),
                shooter,
                config,
                damage,
                spread,
                shooter.getX() + dx / hyp,
                posY,
                shooter.getZ() + dz / hyp,
                dx,
                dy,
                dz);
    }

    private static void rainBolts(BulletBaseMK4Entity orb) {

        for (Player player :
                orb.level().getEntitiesOfClass(Player.class, orb.getBoundingBox().inflate(50D))) {

            Vec3 motion =
                    new Vec3(
                                    player.getX() - orb.getX(),
                                    player.getY() + player.getEyeHeight() - orb.getY(),
                                    player.getZ() - orb.getZ())
                            .normalize();

            BulletBaseMK4Entity bolt =
                    new BulletBaseMK4Entity(
                            orb.level(),
                            orb.getThrower(),
                            maskman_bolt,
                            MASKMAN_BOLT_DAMAGE,
                            0.05F,
                            orb.getX(),
                            orb.getY(),
                            orb.getZ(),
                            motion.x * 0.5D,
                            motion.y * 0.5D,
                            motion.z * 0.5D);
            orb.level().addFreshEntity(bolt);
        }
    }

    private static void summonMeteor(BulletBaseMK4Entity tracer) {

        double y = tracer.getY() + 30 + tracer.getRandom().nextInt(10);
        BulletBaseMK4Entity meteor =
                new BulletBaseMK4Entity(
                        tracer.level(),
                        tracer.getThrower(),
                        maskman_meteor,
                        MASKMAN_METEOR_DAMAGE,
                        0F,
                        tracer.getX(),
                        y,
                        tracer.getZ(),
                        0D,
                        -1D,
                        0D);
        tracer.level().addFreshEntity(meteor);
    }

    private static Entity chooseTarget(BulletBaseMK4Entity bullet) {

        double range = 100D;
        Vec3 motion = bullet.getFlightDirection();
        List<LivingEntity> candidates =
                bullet.level()
                        .getEntitiesOfClass(
                                LivingEntity.class, bullet.getBoundingBox().inflate(range));

        Entity best = null;
        double bestAngle = 90D;

        for (LivingEntity candidate : candidates) {

            if (!candidate.isAlive() || candidate == bullet.getThrower()) continue;
            if (candidate.distanceToSqr(bullet) >= range * range) continue;

            Vec3 eye =
                    new Vec3(
                            candidate.getX(),
                            candidate.getY() + candidate.getBbHeight() / 2D,
                            candidate.getZ());
            if (bullet.level()
                            .clip(
                                    new ClipContext(
                                            bullet.position(),
                                            eye,
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            bullet))
                            .getType()
                    != HitResult.Type.MISS) continue;

            double angle = NtmMath.angleBetween(motion, eye.subtract(bullet.position()));

            if (angle < bestAngle) {
                best = candidate;
                bestAngle = angle;
            }
        }

        return best;
    }
}

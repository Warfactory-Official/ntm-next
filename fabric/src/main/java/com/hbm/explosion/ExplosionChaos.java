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

package com.hbm.explosion;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.particle.CloudFXEntity;
import com.hbm.entity.particle.OrangeFXEntity;
import com.hbm.entity.particle.PinkCloudFXEntity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.Exposure;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.lib.ModDamageTypes;
import com.hbm.platform.Platform;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ArmorRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ExplosionChaos {

    private static final int GAS_SUIT_WEAR = 25;
    private static final float GAS_DAMAGE = 5F;

    private ExplosionChaos() {}

    public static void hardenVirus(Level level, BlockPos center, int radius) {
        var virus = ModBlocks.CRYSTAL_VIRUS.get();
        var hardened = ModBlocks.CRYSTAL_HARDENED.get().defaultBlockState();
        SphereTraversal.forEach(
                center,
                radius,
                cell -> {
                    if (level.getBlockState(cell).is(virus))
                        level.setBlockAndUpdate(cell, hardened);
                });
    }

    public static void floater(Level level, BlockPos center, int radius, int height) {
        var air = Blocks.AIR.defaultBlockState();
        var landing = new BlockPos.MutableBlockPos();
        SphereTraversal.forEach(
                center,
                radius,
                cell -> {
                    var lifted = level.getBlockState(cell);
                    level.setBlockAndUpdate(cell, air);
                    if (!lifted.isAir())
                        level.setBlockAndUpdate(landing.setWithOffset(cell, 0, height, 0), lifted);
                });
    }

    public static void move(Level level, BlockPos center, int radius, int a, int b, int c) {
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double scan = radius + 1D;
        int renameReach = radius * 2;
        double renameSq = (double) renameReach * renameReach;
        double displaceSq = (double) radius * radius;
        var random = level.getRandom();
        var box =
                new AABB(
                        Math.floor(cx - scan),
                        Math.floor(cy - scan),
                        Math.floor(cz - scan),
                        Math.floor(cx + scan),
                        Math.floor(cy + scan),
                        Math.floor(cz + scan));

        var reached =
                EntitySelector.NO_SPECTATORS.and(
                        entity -> entity.distanceToSqr(cx, cy, cz) <= renameSq);

        for (var entity : level.getEntities((Entity) null, box, reached)) {
            if (entity instanceof Sheep) {
                entity.setCustomName(Component.literal("jeb_"));
            } else if (entity instanceof Mob) {
                entity.setCustomName(
                        Component.literal(random.nextInt(2) == 0 ? "Dinnerbone" : "Grumm"));
            }

            if (entity.getEyePosition().distanceToSqr(cx, cy, cz) < displaceSq) {
                entity.setPos(entity.position().add(a, b, c));
            }
        }
    }

    public static void igniteFlammableBlocks(ServerLevel level, int x, int y, int z, int bound) {
        var above = new BlockPos.MutableBlockPos();
        SphereTraversal.forEach(
                new BlockPos(x, y, z),
                bound,
                cell -> {
                    if (!Platform.isFlammable(level, cell, level.getBlockState(cell), Direction.UP))
                        return;
                    if (level.getBlockState(above.setWithOffset(cell, Direction.UP)).isAir())
                        light(level, above);
                });
    }

    public static void igniteAllBlocks(ServerLevel level, int x, int y, int z, int bound) {
        var above = new BlockPos.MutableBlockPos();
        SphereTraversal.forEach(
                new BlockPos(x, y, z),
                bound,
                cell -> {
                    if (level.getBlockState(cell).isAir()) return;
                    BlockState top = level.getBlockState(above.setWithOffset(cell, Direction.UP));
                    if (top.isAir() || top.is(Blocks.SNOW)) light(level, above);
                });
    }

    public static void pinkCloud(Level level, double x, double y, double z, double range) {
        corrode(level, new Vec3(x, y, z), range, ModDamageTypes.PINK_CLOUD, false);
    }

    public static void chlorine(Level level, double x, double y, double z, double range) {
        corrode(level, new Vec3(x, y, z), range, ModDamageTypes.CLOUD, true);
    }

    private static void corrode(
            Level level,
            Vec3 center,
            double range,
            ResourceKey<DamageType> damage,
            boolean hazmatShields) {
        var server = level instanceof ServerLevel s ? s : null;
        var source = level.damageSources().source(damage);
        for (var victim : livingWithin(level, center, range)) {
            ArmorUtil.damageWholeSuit(victim, GAS_SUIT_WEAR);
            if (hazmatShields) {
                if (ArmorUtil.checkForHazmat(victim)) continue;
                if (victim.hasEffect(HbmPotion.taint())) {
                    victim.removeEffect(HbmPotion.taint());
                    victim.addEffect(
                            new MobEffectInstance(
                                    HbmPotion.mutation(), 60 * 60 * 20, 0, false, false));
                }
            }
            if (server != null) victim.hurtServer(server, source, GAS_DAMAGE);
        }
    }

    public static void poison(Level level, double x, double y, double z, double range) {
        for (var victim : livingWithin(level, new Vec3(x, y, z), range)) {
            if (ArmorRegistry.hasProtection(victim, EquipmentSlot.HEAD, Exposure.GAS_LUNG)
                    || ArmorRegistry.hasProtection(
                            victim, EquipmentSlot.HEAD, Exposure.GAS_BLISTERING)) {
                ArmorUtil.damageGasMaskFilter(victim, 1);
                continue;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30 * 20, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 30 * 20, 2));
        }
    }

    private static List<LivingEntity> livingWithin(Level level, Vec3 center, double range) {
        double rangeSq = range * range;
        double span = range * 2D;
        return level.getEntitiesOfClass(
                LivingEntity.class,
                AABB.ofSize(center, span, span, span),
                entity -> !entity.isSpectator() && entity.distanceToSqr(center) <= rangeSq);
    }

    private static void light(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, BaseFireBlock.getState(level, pos), Block.UPDATE_ALL);
    }

    public static void spawnPoisonCloud(
            Level level, double x, double y, double z, int count, double speed, int type) {
        var random = level.getRandom();
        while (count-- > 0) {
            Entity puff =
                    switch (type) {
                        case 1 -> new CloudFXEntity(level, x, y, z, 0D, 0D, 0D);
                        case 2 -> new PinkCloudFXEntity(level, x, y, z, 0D, 0D, 0D);
                        default -> new OrangeFXEntity(level, x, y, z, 0D, 0D, 0D);
                    };
            double rise = random.nextGaussian() * speed;
            puff.setDeltaMovement(
                    random.nextGaussian() * speed, rise, random.nextGaussian() * speed);
            level.addFreshEntity(puff);
        }
    }

    public static void cluster(
            Level level,
            double x,
            double y,
            double z,
            int count,
            float yaw,
            float pitch,
            float yawRand,
            float pitchRand,
            float speed) {
        var random = level.getRandom();
        while (count-- > 0) {
            float heading = yaw + (float) (yawRand * random.nextGaussian());
            float elevation = pitch + (float) (pitchRand * random.nextGaussian());
            var shard =
                    new BulletBaseMK4Entity(
                            level,
                            XFactoryCatapult.cluster_submunition,
                            50F,
                            0F,
                            heading,
                            elevation);
            shard.setDeltaMovement(shard.getDeltaMovement().scale(speed));
            shard.setPos(x, y, z);
            level.addFreshEntity(shard);
        }
    }
}

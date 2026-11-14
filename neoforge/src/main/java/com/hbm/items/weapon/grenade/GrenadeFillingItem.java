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

package com.hbm.items.weapon.grenade;

import com.hbm.NuclearTech;
import com.hbm.entity.effect.CloudFleijaEntity;
import com.hbm.entity.effect.FireLingeringEntity;
import com.hbm.entity.grenade.GrenadeUniversalEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.entity.projectile.BulletBeamBaseEntity;
import com.hbm.explosion.ElectromagneticPulse;
import com.hbm.explosion.MiniNukeAftermath;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.grenade.GrenadeFillingDefinition;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.HazeLayout;
import com.hbm.packet.layout.PlasmaBlastLayout;
import com.hbm.platform.Platform;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GrenadeFillingItem extends Item {

    public static BulletConfig fragmentation;
    public static BulletConfig pellets;
    public static BulletConfig pellets_heavy;
    public static BulletConfig laser;

    public final GrenadeFillingDefinition type;

    public GrenadeFillingItem(Item.Properties properties, GrenadeFillingDefinition type) {
        super(properties);
        this.type = type;
    }

    public static void init() {
        fragmentation =
                new BulletConfig()
                        .setLife(3)
                        .setThresholdNegation(5F)
                        .setRicochetAngle(90)
                        .setRicochetCount(2);
        pellets =
                new BulletConfig()
                        .setLife(100)
                        .setGrav(0.04D)
                        .setVel(1.5F)
                        .setOnImpact(
                                (bullet, impact) -> {
                                    if (bullet.tickCount < 2) return;
                                    Lego.tinyExplode(bullet, impact, 1.5F);
                                    bullet.discard();
                                });
        pellets_heavy =
                new BulletConfig()
                        .setLife(100)
                        .setGrav(0.04D)
                        .setVel(1.5F)
                        .setOnImpact(
                                (bullet, impact) -> {
                                    if (bullet.tickCount < 2) return;
                                    Lego.standardExplode(bullet, impact, 5F);
                                    bullet.discard();
                                });
        laser =
                new BulletConfig()
                        .setBeam()
                        .setupDamageClass(ModDamageTypes.LASER)
                        .setLife(3)
                        .setRenderRotations(false)
                        .setThresholdNegation(10F)
                        .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
    }

    public static void explode(GrenadeUniversalEntity grenade) {
        grenade.getFilling().explode(grenade);
    }

    private static void explodeNative(GrenadeUniversalEntity grenade, EnumGrenadeFilling filling) {
        switch (filling) {
            case POWDER -> standardExplode(grenade, 5F, 10F, 5F, 0F);
            case HE -> standardExplode(grenade, 7.5F, 25F, 10F, 0.1F);
            case DEMO -> explodeDemo(grenade);
            case INC -> explodeIncendiary(grenade);
            case WP -> explodeWhitePhosphorus(grenade);
            case CLUSTER -> explodeCluster(grenade);
            case EMP -> explodeEmp(grenade);
            case PLASMA ->
                    explodeStandardEnergy(
                            grenade, 50F, 5F, ModDamageTypes.PLASMA, 0.5F, 1F, 0.5F, 4F);
            case LASER -> explodeLaser(grenade);
            case CLUSTER_HEAVY -> explodeClusterHeavy(grenade);
            case NUCLEAR -> explodeNuclear(grenade);
            case NUCLEAR_DEMO -> explodeNuclearDemo(grenade);
            case SCHRAB -> explodeSchrabidium(grenade);
        }
    }

    private static void explodeDemo(GrenadeUniversalEntity grenade) {
        ComposableExplosion vnt =
                new ComposableExplosion(
                        grenade.level(),
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        5F,
                        grenade.getThrower());
        vnt.blockSelector(new AttenuatedRayBlockSelector());
        vnt.blockProcessor(CellDestruction.decay());
        vnt.entityProcessor(new WeaponExplosionEntityProcessor(1, 10F));
        vnt.effects(new SmallExplosionEffect(10, 2.5F, 1F));
        vnt.explode();
    }

    private static void explodeCluster(GrenadeUniversalEntity grenade) {
        standardExplode(grenade, 7.5F, 15F, 10F, 0.1F);
        int frags = 30;
        if (grenade.getShell() == GrenadeShellItem.EnumGrenadeShell.FRAG) frags *= 1.25;
        for (int i = 0; i < frags; i++) {
            BulletBaseMK4Entity bullet =
                    new BulletBaseMK4Entity(
                            grenade.level(),
                            pellets,
                            15F,
                            0F,
                            grenade.level().getRandom().nextFloat() * 2F * (float) Math.PI,
                            (grenade.level().getRandom().nextFloat() * 0.5F + 0.5F)
                                    * (float) Math.PI);
            bullet.setPos(grenade.getX(), grenade.getY() + 0.05D, grenade.getZ());
            bullet.setDeltaMovement(bullet.getDeltaMovement().multiply(0.5D, 0.75D, 0.5D));
            grenade.level().addFreshEntity(bullet);
        }
    }

    private static void explodeClusterHeavy(GrenadeUniversalEntity grenade) {
        standardExplode(grenade, 7.5F, 15F, 10F, 0.1F);
        for (int i = 0; i < 15; i++) {
            BulletBaseMK4Entity bullet =
                    new BulletBaseMK4Entity(
                            grenade.level(),
                            pellets_heavy,
                            30F,
                            0F,
                            grenade.level().getRandom().nextFloat() * 2F * (float) Math.PI,
                            (grenade.level().getRandom().nextFloat() * 0.5F + 0.5F)
                                    * (float) Math.PI);
            bullet.setPos(grenade.getX(), grenade.getY() + 0.05D, grenade.getZ());
            bullet.setDeltaMovement(bullet.getDeltaMovement().multiply(0.5D, 1.25D, 0.5D));
            grenade.level().addFreshEntity(bullet);
        }
    }

    private static void explodeIncendiary(GrenadeUniversalEntity grenade) {
        Level level = grenade.level();
        standardExplode(grenade, 3F, 10F, 0F, 0F);
        FireLingeringEntity fire =
                new FireLingeringEntity(level)
                        .setArea(6, 2)
                        .setDuration(200)
                        .setType(FireLingeringEntity.TYPE_DIESEL);
        fire.setPos(grenade.getX(), grenade.getY(), grenade.getZ());
        level.addFreshEntity(fire);
        igniteAround(grenade, 2);
    }

    private static void explodeWhitePhosphorus(GrenadeUniversalEntity grenade) {
        Level level = grenade.level();
        standardExplode(grenade, 3F, 10F, 0F, 0F);
        FireLingeringEntity fire =
                new FireLingeringEntity(level)
                        .setArea(6, 2)
                        .setDuration(600)
                        .setType(FireLingeringEntity.TYPE_PHOSPHORUS);
        fire.setPos(grenade.getX(), grenade.getY(), grenade.getZ());
        level.addFreshEntity(fire);
        igniteAround(grenade, 3);
        if (!(level instanceof ServerLevel server)) return;
        for (int i = 0; i < 3; i++) {
            PacketWire.sendToAllAround(
                    HazeLayout.encode(
                            grenade.getX() + server.getRandom().nextGaussian() * 4D,
                            grenade.getY(),
                            grenade.getZ() + server.getRandom().nextGaussian() * 4D),
                    new TargetPoint(server, grenade.getX(), grenade.getY(), grenade.getZ(), 150));
        }
    }

    private static void igniteAround(GrenadeUniversalEntity grenade, int radius) {
        if (!(grenade.level() instanceof ServerLevel server)) return;
        BlockPos origin = BlockPos.containing(grenade.getX(), grenade.getY(), grenade.getZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++)
            for (int dy = -radius; dy <= radius; dy++)
                for (int dz = -radius; dz <= radius; dz++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!server.getBlockState(pos).isAir()) continue;
                    for (Direction direction : Direction.VALUES) {
                        BlockPos neighbor = pos.relative(direction);
                        if (Platform.isFlammable(
                                server,
                                neighbor,
                                server.getBlockState(neighbor),
                                direction.getOpposite())) {
                            server.setBlock(pos, BaseFireBlock.getState(server, pos), 3);
                            break;
                        }
                    }
                }
    }

    private static void explodeEmp(GrenadeUniversalEntity grenade) {
        explodeStandardEnergy(grenade, 15F, 3F, ModDamageTypes.ELECTRIC, 0.5F, 0.5F, 1F, 3F);
        ElectromagneticPulse.sphere(
                grenade.level(),
                BlockPos.containing(grenade.getX(), grenade.getY(), grenade.getZ()),
                5);
    }

    private static void explodeLaser(GrenadeUniversalEntity grenade) {
        tinyExplode(grenade, 2F, 5F);

        double x = grenade.getX();
        double y = grenade.getY() + 0.125D;
        double z = grenade.getZ();

        double range = 15D;
        List<LivingEntity> potentialTargets =
                grenade.level()
                        .getEntitiesOfClass(
                                LivingEntity.class, new AABB(x, y, z, x, y, z).inflate(range));
        Collections.shuffle(potentialTargets);

        for (LivingEntity target : potentialTargets) {
            if (target == grenade.getThrower()) continue;

            Vec3 delta =
                    new Vec3(
                            target.getX() - x,
                            target.getY() + target.getBbHeight() / 2F - y,
                            target.getZ() - z);
            if (delta.length() > range) continue;
            BulletBeamBaseEntity sub = new BulletBeamBaseEntity(grenade.level(), laser, 30F);
            sub.setOwner(grenade.getThrower());
            sub.setPos(x, y, z);
            sub.setRotationsFromVector(delta);
            sub.performHitscanExternal(delta.length());
            grenade.level().addFreshEntity(sub);
        }
    }

    private static void explodeNuclear(GrenadeUniversalEntity grenade) {
        ComposableExplosion vnt =
                new ComposableExplosion(
                        grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 10F);
        vnt.entityProcessor(new WeaponExplosionEntityProcessor(2, 100).range(1.5F));
        vnt.explode();
        MiniNukeAftermath.fallout(grenade.level(), grenade.position(), 1F);
        spawnMush(grenade);
    }

    private static void explodeNuclearDemo(GrenadeUniversalEntity grenade) {
        ComposableExplosion vnt =
                new ComposableExplosion(
                        grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 10F);
        vnt.blockSelector(new AttenuatedRayBlockSelector(64));
        vnt.blockProcessor(CellDestruction.decay().transform(new BlockMutatorFire()));
        vnt.entityProcessor(new WeaponExplosionEntityProcessor(2, 50).range(1.5F));
        vnt.explode();
        MiniNukeAftermath.fallout(grenade.level(), grenade.position(), 1.5F);
        spawnMush(grenade);
    }

    private static void explodeSchrabidium(GrenadeUniversalEntity grenade) {
        Level level = grenade.level();
        NukeExplosionMK3Entity ex =
                NukeExplosionMK3Entity.statFacFleija(
                        level, grenade.getX(), grenade.getY(), grenade.getZ(), 20);
        if (!ex.isRemoved()) {
            WorldSounds.play(
                    level,
                    null,
                    grenade.getX(),
                    grenade.getY(),
                    grenade.getZ(),
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    100.0F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.addFreshEntity(ex);
            level.addFreshEntity(
                    CloudFleijaEntity.statFac(
                            level, 20, grenade.getX(), grenade.getY(), grenade.getZ()));
        }
    }

    public static void spawnMush(GrenadeUniversalEntity grenade) {
        var level = grenade.level();
        var at = grenade.position();
        MiniNukeAftermath.report(level, at);
        MiniNukeAftermath.sound(level, at, 15.0F, 1.0F);
        MiniNukeAftermath.rollCloud(level, at, 0.5D);
    }

    public static void explodeStandardEnergy(
            GrenadeUniversalEntity grenade,
            float damage,
            float range,
            ResourceKey<DamageType> damageClass,
            float r,
            float g,
            float b,
            float scale) {
        ComposableExplosion vnt =
                new ComposableExplosion(
                        grenade.level(),
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        range,
                        grenade.getThrower());
        vnt.entityProcessor(new WeaponExplosionEntityProcessor(1, damage).damageType(damageClass));
        vnt.explode();
        WorldSounds.play(
                grenade.level(),
                null,
                grenade.getX(),
                grenade.getY(),
                grenade.getZ(),
                ModSounds.UFO_BLAST.get(),
                SoundSource.BLOCKS,
                5F,
                0.9F + grenade.level().getRandom().nextFloat() * 0.2F);
        WorldSounds.play(
                grenade.level(),
                null,
                grenade.getX(),
                grenade.getY(),
                grenade.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.BLOCKS,
                5F,
                0.5F);

        if (!(grenade.level() instanceof ServerLevel server)) return;
        float yaw = server.getRandom().nextFloat() * 180F;
        for (int i = 0; i < 3; i++) {
            PacketWire.sendToAllAround(
                    PlasmaBlastLayout.encode(
                            grenade.getX(),
                            grenade.getY() + 0.125D,
                            grenade.getZ(),
                            r,
                            g,
                            b,
                            -60F + 60F * i,
                            yaw,
                            scale),
                    new TargetPoint(server, grenade.getX(), grenade.getY(), grenade.getZ(), 100));
        }
    }

    public static void standardExplode(
            GrenadeUniversalEntity grenade,
            float range,
            float damage,
            float threshold,
            float resistance) {
        ComposableExplosion vnt =
                new ComposableExplosion(
                        grenade.level(),
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        range,
                        grenade.getThrower());
        vnt.entityProcessor(
                new WeaponExplosionEntityProcessor(1, damage).piercing(threshold, resistance));
        vnt.effects(new SmallExplosionEffect(10, 2.5F, 1F));
        vnt.explode();
    }

    public static void tinyExplode(GrenadeUniversalEntity grenade, float range, float damage) {
        ComposableExplosion vnt =
                new ComposableExplosion(
                        grenade.level(),
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        range,
                        grenade.getThrower());
        vnt.entityProcessor(new WeaponExplosionEntityProcessor(0.5, damage).knockback(0.25D));
        vnt.effects(new ExplosionEffectTiny());
        vnt.explode();
    }

    public static void standardFragmentation(GrenadeUniversalEntity grenade, float frags) {
        if (grenade.getShell() == GrenadeShellItem.EnumGrenadeShell.FRAG) frags *= 1.5F;
        for (int i = 0; i < frags; i++) {
            BulletBaseMK4Entity bullet =
                    new BulletBaseMK4Entity(
                            grenade.level(),
                            fragmentation,
                            10F,
                            0F,
                            grenade.level().getRandom().nextFloat() * 2F * (float) Math.PI,
                            (grenade.level().getRandom().nextFloat() - 0.5F)
                                    * 2F
                                    * (float) Math.PI);
            bullet.setPos(grenade.getX(), grenade.getY() + 0.05D, grenade.getZ());
            grenade.level().addFreshEntity(bullet);
        }
    }

    public enum EnumGrenadeFilling implements GrenadeFillingDefinition {
        POWDER(0x424242, 0x939176),
        HE(0x595533, 0xA49D62),
        DEMO(0x595533, 0xDD4029),
        INC(0x5A5A5A, 0xFF5F21),
        WP(0xDCDCDC, 0xFF5F21),
        CLUSTER(0x5A5A5A, 0xFFC711),
        EMP(0x93A1AC, 0x00FFFF),
        PLASMA(0x655B2C, 0x4CFF00),
        LASER(0x493A3A, 0xFF0000),
        CLUSTER_HEAVY(0x5A5A5A, 0xFF5F21),
        NUCLEAR(0xDFD7A8, 0xA49D62),
        NUCLEAR_DEMO(0xDFD7A8, 0xDD4029),
        SCHRAB(0x00BDBD, 0x000000);

        private final Identifier id;
        private final int bodyColor;
        private final int labelColor;

        EnumGrenadeFilling(int bodyColor, int labelColor) {
            this.id =
                    Identifier.fromNamespaceAndPath(
                            NuclearTech.MOD_ID, name().toLowerCase(Locale.ROOT));
            this.bodyColor = bodyColor;
            this.labelColor = labelColor;
        }

        @Override
        public Identifier id() {
            return id;
        }

        @Override
        public GrenadeFillingItem item() {
            return ModItems.GRENADE_FILLING.get(this);
        }

        @Override
        public void explode(GrenadeUniversalEntity grenade) {
            explodeNative(grenade, this);
        }

        @Override
        public boolean compatible(GrenadeShellItem.EnumGrenadeShell shell) {
            return switch (this) {
                case POWDER, HE, DEMO, INC, WP, CLUSTER ->
                        shell == GrenadeShellItem.EnumGrenadeShell.FRAG
                                || shell == GrenadeShellItem.EnumGrenadeShell.STICK;
                case EMP, PLASMA, LASER -> shell == GrenadeShellItem.EnumGrenadeShell.TECH;
                case CLUSTER_HEAVY, NUCLEAR, NUCLEAR_DEMO, SCHRAB ->
                        shell == GrenadeShellItem.EnumGrenadeShell.NUKE;
            };
        }

        @Override
        public int bodyColor() {
            return bodyColor;
        }

        @Override
        public int labelColor() {
            return labelColor;
        }
    }
}

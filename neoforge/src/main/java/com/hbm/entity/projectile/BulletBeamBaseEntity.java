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

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.CoinEntity;
import com.hbm.entity.projectile.ProjectilePath;
import com.hbm.items.weapon.sedna.BulletConfig;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;

public class BulletBeamBaseEntity extends Projectile {

    private static final EntityDataAccessor<Integer> CONFIG_ID =
            SynchedEntityData.defineId(BulletBeamBaseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BEAM_LENGTH =
            SynchedEntityData.defineId(BulletBeamBaseEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> EXACT_YAW =
            SynchedEntityData.defineId(BulletBeamBaseEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> EXACT_PITCH =
            SynchedEntityData.defineId(BulletBeamBaseEntity.class, EntityDataSerializers.FLOAT);
    public BulletConfig config;
    public float damage;
    public double headingX;
    public double headingY;
    public double headingZ;

    public BulletBeamBaseEntity(EntityType<? extends BulletBeamBaseEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public BulletBeamBaseEntity(Level level, BulletConfig config, float baseDamage) {
        this(ModEntities.BULLET_BEAM.get(), level);
        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;
    }

    public BulletBeamBaseEntity(LivingEntity entity, BulletConfig config, float baseDamage) {
        this(entity.level(), config, baseDamage);
        setOwner(entity);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        double range = getBoundingBox().getSize() * 64.0D * 10.0D * getViewScale();
        return distSq < range * range;
    }

    public BulletBeamBaseEntity(
            LivingEntity entity,
            BulletConfig config,
            float baseDamage,
            float angularInaccuracy,
            double sideOffset,
            double heightOffset,
            double frontOffset) {
        this(ModEntities.BULLET_BEAM.get(), entity.level());

        setOwner(entity);
        this.setBulletConfig(config);

        this.damage = baseDamage * this.config.damageMult;

        this.snapTo(
                entity.getX(),
                entity.getY() + entity.getEyeHeight(),
                entity.getZ(),
                entity.getYRot() + (float) this.random.nextGaussian() * angularInaccuracy,
                entity.getXRot() + (float) this.random.nextGaussian() * angularInaccuracy);

        Vec3 offset =
                new Vec3(sideOffset, heightOffset, frontOffset)
                        .xRot(-this.getXRot() / 180F * (float) Math.PI)
                        .yRot(-this.getYRot() / 180F * (float) Math.PI);

        this.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);

        this.headingX =
                -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingZ =
                Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);

        double range = 250D;
        this.headingX *= range;
        this.headingY *= range;
        this.headingZ *= range;

        this.entityData.set(EXACT_YAW, this.getYRot());
        this.entityData.set(EXACT_PITCH, this.getXRot());

        performHitscan();
    }

    public LivingEntity getThrower() {
        return getOwner() instanceof LivingEntity living ? living : null;
    }

    public void setRotationsFromVector(Vec3 delta) {
        this.setXRot((float) (-Math.asin(delta.y / delta.length()) * 180D / Math.PI));
        this.setYRot((float) (-Math.atan2(delta.x, delta.z) * 180D / Math.PI));

        this.headingX =
                -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingZ =
                Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        this.headingY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);

        this.entityData.set(EXACT_YAW, this.getYRot());
        this.entityData.set(EXACT_PITCH, this.getXRot());
    }

    public void performHitscanExternal(double range) {
        this.headingX *= range;
        this.headingY *= range;
        this.headingZ *= range;
        performHitscan();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONFIG_ID, 0);
        builder.define(BEAM_LENGTH, 0F);
        builder.define(EXACT_YAW, 0F);
        builder.define(EXACT_PITCH, 0F);
    }

    public BulletConfig getBulletConfig() {
        int id = this.entityData.get(CONFIG_ID);
        return BulletConfig.configs.get(id);
    }

    public void setBulletConfig(BulletConfig config) {
        this.config = config;
        this.entityData.set(CONFIG_ID, config.id);
    }

    public double getBeamLength() {
        return this.entityData.get(BEAM_LENGTH);
    }

    public float getExactYaw() {
        return this.entityData.get(EXACT_YAW);
    }

    public float getExactPitch() {
        return this.entityData.get(EXACT_PITCH);
    }

    @Override
    public void tick() {

        if (config == null) config = this.getBulletConfig();

        if (config.onUpdate != null) config.onUpdate.accept(this);
        if (isRemoved()) return;

        super.tick();

        if (!this.level().isClientSide() && this.tickCount > config.expires) this.discard();
    }

    protected void performHitscan() {
        if (!(level() instanceof ServerLevel)) return;
        setDeltaMovement(headingX, headingY, headingZ);
        super.tick();
        var from = position();
        var end = from.add(getDeltaMovement());
        var block =
                isSpectral()
                        ? BlockHitResult.miss(end, Direction.UP, BlockPos.containing(end))
                        : traceBlocks(from, end);
        var to = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        if (doesImpactEntities()) {
            var movement = getDeltaMovement();
            var hits = traceEntities(from, to);
            if (!doesPenetrate()) {
                hits.sort(
                        Comparator.comparingInt(
                                hit -> hit.getEntity() instanceof CoinEntity ? 0 : 1));
            }
            for (var hit : hits) {
                entityData.set(BEAM_LENGTH, (float) from.distanceTo(hit.getLocation()));
                var deflection = ProjectilePath.impact(this, hit);
                if (!isAlive()) return;
                if (deflection == null) continue;
                if (deflection != ProjectileDeflection.NONE
                        || hit.getEntity() instanceof CoinEntity
                        || !doesPenetrate()
                        || !getDeltaMovement().equals(movement)) return;
            }
        }
        entityData.set(BEAM_LENGTH, (float) from.distanceTo(to));
        if (block.getType() != HitResult.Type.MISS && canHitBlock(block))
            ProjectilePath.impact(this, block);
    }

    protected BlockHitResult traceBlocks(Vec3 from, Vec3 to) {
        return level().clipIncludingBorder(
                        new ClipContext(
                                from,
                                to,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                this));
    }

    protected boolean canHitBlock(BlockHitResult hit) {
        return true;
    }

    protected List<EntityHitResult> traceEntities(Vec3 from, Vec3 to) {
        return isSpectral()
                ? ProjectilePath.entitiesUnobstructed(this, from, to, this::canHitEntity, 0.3F)
                : ProjectilePath.entities(this, from, to, this::canHitEntity, 0.3F);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (hit.getEntity() instanceof CoinEntity coin) {
            redirectCoin(coin, hit.getLocation());
        } else {
            onImpact(hit);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        onImpact(hit);
    }

    protected BulletBeamBaseEntity createRicochet(LivingEntity shooter, float baseDamage) {
        return new BulletBeamBaseEntity(shooter, config, baseDamage);
    }

    private void redirectCoin(CoinEntity hitCoin, Vec3 coinHit) {
        double range = 50;
        List<Entity> targets =
                this.level()
                        .getEntities(
                                (Entity) null,
                                new AABB(
                                                coinHit.x, coinHit.y, coinHit.z, coinHit.x,
                                                coinHit.y, coinHit.z)
                                        .inflate(range, range, range));
        Entity nearestCoin = null;
        Entity nearestPlayer = null;
        Entity nearestMob = null;
        Entity nearestOther = null;
        double coinDist = 0;
        double playerDist = 0;
        double mobDist = 0;
        double otherDist = 0;

        hitCoin.discard();

        for (Entity entity : targets) {
            if (entity == getThrower()) continue;
            if (!entity.isAlive()) continue;
            double dist = entity.distanceTo(hitCoin);
            if (dist > range) continue;

            if (entity instanceof CoinEntity) {
                if (coinDist == 0 || dist < coinDist) {
                    coinDist = dist;
                    nearestCoin = entity;
                }
            } else if (entity instanceof Player) {
                if (playerDist == 0 || dist < playerDist) {
                    playerDist = dist;
                    nearestPlayer = entity;
                }
            } else if (entity instanceof Monster) {
                if (mobDist == 0 || dist < mobDist) {
                    mobDist = dist;
                    nearestMob = entity;
                }
            } else if (entity instanceof LivingEntity) {
                if (otherDist == 0 || dist < otherDist) {
                    otherDist = dist;
                    nearestOther = entity;
                }
            }
        }

        Entity target =
                nearestCoin != null
                        ? nearestCoin
                        : nearestPlayer != null
                                ? nearestPlayer
                                : nearestMob != null ? nearestMob : nearestOther;

        BulletBeamBaseEntity newBeam =
                createRicochet(
                        hitCoin.getThrower() != null ? hitCoin.getThrower() : getThrower(),
                        damage * 1.25F);
        newBeam.setPos(coinHit);
        if (target != null) {
            Vec3 delta =
                    new Vec3(
                            target.getX() - newBeam.getX(),
                            (target.getY() + target.getBbHeight() / 2D) - newBeam.getY(),
                            target.getZ() - newBeam.getZ());
            newBeam.setRotationsFromVector(delta);
        } else {
            newBeam.setRotationsFromVector(
                    new Vec3(
                            this.random.nextGaussian() * 0.5,
                            -1,
                            this.random.nextGaussian() * 0.5));
        }
        newBeam.performHitscanExternal(250D);
        this.level().addFreshEntity(newBeam);

        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.EXPLOSION, coinHit.x, coinHit.y, coinHit.z, 1, 0, 0, 0, 0);
        }
    }

    protected void onImpact(HitResult mop) {
        if (!this.level().isClientSide()) {
            if (this.config.onImpactBeam != null) this.config.onImpactBeam.accept(this, mop);
        }
    }

    public boolean doesImpactEntities() {
        return this.config.impactsEntities;
    }

    public boolean doesPenetrate() {
        return this.config.doesPenetrate;
    }

    public boolean isSpectral() {
        return this.config.isSpectral;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.discard();
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}

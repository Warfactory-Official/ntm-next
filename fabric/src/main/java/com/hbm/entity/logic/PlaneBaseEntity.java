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

package com.hbm.entity.logic;

import com.hbm.client.ClientEffects;
import com.hbm.entity.FarVisible;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.helper.ExplosionSmallCreator;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ChunkUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class PlaneBaseEntity extends Entity implements FarVisible {

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(PlaneBaseEntity.class, EntityDataSerializers.FLOAT);
    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    public float health = getMaxHealth();
    public int timer = getLifetime();
    public float renderPitch = -90F;
    public float renderPitchO = -90F;
    public float renderYaw;
    public float renderYawO;

    public PlaneBaseEntity(EntityType<? extends PlaneBaseEntity> type, Level level) {
        super(type, level);
    }

    public float getMaxHealth() {
        return 50F;
    }

    public int getLifetime() {
        return 200;
    }

    @Override
    public boolean isPickable() {
        return this.health > 0;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HEALTH, 50F);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public void tick() {
        Vec3 prePos = position();
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) {
            this.health = this.entityData.get(DATA_HEALTH);
            Vec3 step = position().subtract(prePos);
            updateRenderRotation(step);
            if (this.health <= 0) {
                for (int i = 0; i < 10; i++) {
                    ClientEffects.spawnPlaneFlame(
                            level(),
                            getX() + this.random.nextGaussian() * 0.5 - step.x * 2,
                            getY() + this.random.nextGaussian() * 0.5 - step.y * 2,
                            getZ() + this.random.nextGaussian() * 0.5 - step.z * 2);
                }
            }
            return;
        }

        this.entityData.set(DATA_HEALTH, this.health);

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        rotation();

        if (this.health <= 0) {
            setDeltaMovement(motion.x, motion.y - 0.025, motion.z);

            if (!level().getBlockState(blockPosition()).isAir() || getY() < level().getMinY()) {
                double x = getX(), y = getY(), z = getZ();
                discard();
                new ComposableExplosion(level(), x, y, z, 15F).standard().explode();
                WorldSounds.play(
                        level(),
                        null,
                        x,
                        y,
                        z,
                        ModSounds.ENTITY_PLANE_CRASH.get(),
                        SoundSource.NEUTRAL,
                        25.0F,
                        1.0F);
                return;
            }
        } else {
            setDeltaMovement(motion.x, 0, motion.z);
        }

        if (this.tickCount > this.timer) {
            discard();
            return;
        }
        ChunkUtil.holdOwnChunk(this);
    }

    protected void rotation() {
        Vec3 motion = getDeltaMovement();
        setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
    }

    private void updateRenderRotation(Vec3 step) {
        this.renderPitchO = this.renderPitch;
        this.renderYawO = this.renderYaw;
        double horiz = Math.sqrt(step.x * step.x + step.z * step.z);
        if (step.lengthSqr() > 1.0e-8) {
            this.renderPitch = (float) (Math.atan2(step.y, horiz) * 180.0D / Math.PI) - 90F;
        }
        if (horiz > 1.0e-3) {
            this.renderYaw = (float) (Math.atan2(step.x, step.z) * 180.0D / Math.PI);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(ModDamageTypes.NUCLEAR_BLAST)) return false;
        if (isInvulnerableToBase(source)) return false;
        if (!isRemoved() && this.health > 0) {
            this.health -= amount;
            if (this.health <= 0) killPlane();
        }
        return true;
    }

    protected void killPlane() {
        ExplosionSmallCreator.composeEffect(level(), getX(), getY(), getZ(), 25, 3.5F, 2F);
        WorldSounds.play(
                level(),
                null,
                getX(),
                getY(),
                getZ(),
                ModSounds.ENTITY_PLANE_SHOT_DOWN.get(),
                SoundSource.NEUTRAL,
                25.0F,
                1.0F);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.tickCount = input.getIntOr("ticksExisted", 0);
        this.health = input.getFloatOr("health", getMaxHealth());
        this.entityData.set(DATA_HEALTH, this.health);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("ticksExisted", this.tickCount);
        output.putFloat("health", this.health);
    }

    public void forceSpawnChunk() {
        if (level() instanceof ServerLevel server)
            ChunkUtil.loadTickingForEntity(server, chunkPosition());
    }
}

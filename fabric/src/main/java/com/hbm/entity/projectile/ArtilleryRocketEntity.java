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

import com.hbm.api.entity.ArtilleryRadarProvider;
import com.hbm.client.ClientEffects;
import com.hbm.entity.FarVisible;
import com.hbm.entity.projectile.InterpolatedProjectileEntity;
import com.hbm.entity.projectile.rocketbehavior.RocketGuidance;
import com.hbm.entity.projectile.rocketbehavior.RocketSteeringBallisticArc;
import com.hbm.entity.projectile.rocketbehavior.RocketTargetingPredictive;
import com.hbm.handler.resistance.DamageCategory;
import com.hbm.items.weapon.AmmoHIMARSItem;
import com.hbm.util.ChunkUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ArtilleryRocketEntity extends InterpolatedProjectileEntity
        implements ArtilleryRadarProvider, FarVisible {

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (amount >= 250F && DamageCategory.of(source) == DamageCategory.EN) discard();
        return false;
    }

    private static final EntityDataAccessor<Integer> ROCKET_TYPE =
            SynchedEntityData.defineId(ArtilleryRocketEntity.class, EntityDataSerializers.INT);

    private @Nullable EntityReference<Entity> target;
    public Vec3 lastTargetPos;

    public RocketGuidance.Aim targeting;
    public RocketGuidance.Steer steering;

    public ArtilleryRocketEntity(EntityType<? extends ArtilleryRocketEntity> type, Level level) {
        super(type, level);
        this.targeting = new RocketTargetingPredictive();
        this.steering = new RocketSteeringBallisticArc();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROCKET_TYPE, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public ArtilleryRocketEntity setType(int type) {
        this.entityData.set(ROCKET_TYPE, type);
        return this;
    }

    public int getTypeIndex() {
        return this.entityData.get(ROCKET_TYPE);
    }

    public AmmoHIMARSItem.HIMARSRocketType getRocket() {
        return AmmoHIMARSItem.HIMARSRocketType.VALUES[getTypeIndex()];
    }

    public ArtilleryRocketEntity setTarget(Entity target) {
        this.target = EntityReference.of(target);
        setTarget(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
        return this;
    }

    public ArtilleryRocketEntity setTarget(double x, double y, double z) {
        this.lastTargetPos = new Vec3(x, y, z);
        return this;
    }

    public Vec3 getLastTarget() {
        return this.lastTargetPos;
    }

    public @Nullable Entity getTargetEntity() {
        return target == null ? null : target.getEntity(level()::getEntity, Entity.class);
    }

    @Override
    public void tick() {
        var previous = flightStart();
        super.tick();

        if (this.isRemoved()) return;

        if (level().isClientSide()) {
            Vec3 back = previous.subtract(position());
            double velocity = back.length();

            if (velocity > 1D) {
                Vec3 v = back.normalize();
                for (int i = 6; i < velocity + 6; i++) {
                    ClientEffects.spawnContrail(
                            level(),
                            getX() + v.x * i,
                            getY() + v.y * i,
                            getZ() + v.z * i,
                            ClientEffects.Contrail.KEROSENE);
                }
            }
            return;
        }

        Vec3 delta =
                new Vec3(
                        this.lastTargetPos.x - this.getX(),
                        this.lastTargetPos.y - this.getY(),
                        this.lastTargetPos.z - this.getZ());
        double momentum =
                Math.sqrt(
                        getDeltaMovement().x * getDeltaMovement().x
                                + getDeltaMovement().y * getDeltaMovement().y
                                + getDeltaMovement().z * getDeltaMovement().z);
        Entity targetEntity = getTargetEntity();
        if (targetEntity != null && targetEntity.isSpectator()) {
            target = null;
            targetEntity = null;
        }
        if (delta.length() <= momentum * 1.5) {
            if (targetEntity == null || !targetEntity.isAlive()) {
                this.targeting = null;
                this.steering = null;
            }
            delta = delta.normalize();
            this.setDeltaMovement(delta.x * momentum, delta.y * momentum, delta.z * momentum);
        } else {
            if (this.targeting != null && targetEntity != null)
                this.targeting.retarget(this, targetEntity);
            if (this.steering != null) this.steering.steer(this, 25D, 15D);
        }

        ChunkUtil.holdOwnChunk(this);
        getRocket().onUpdate(this);
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (!level().isClientSide()) {
            getRocket().onImpact(this, mop);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        this.lastTargetPos =
                new Vec3(
                        input.getDoubleOr("targetX", 0D),
                        input.getDoubleOr("targetY", 0D),
                        input.getDoubleOr("targetZ", 0D));

        this.entityData.set(ROCKET_TYPE, input.getIntOr("type", 0));
        this.target = EntityReference.read(input, "target");
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        if (this.lastTargetPos == null) {
            this.lastTargetPos = new Vec3(getX(), getY(), getZ());
        }

        output.putDouble("targetX", this.lastTargetPos.x);
        output.putDouble("targetY", this.lastTargetPos.y);
        output.putDouble("targetZ", this.lastTargetPos.z);

        output.putInt("type", this.entityData.get(ROCKET_TYPE));
        EntityReference.store(this.target, output, "target");
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    public double getGravityVelocity() {
        return this.steering != null ? 0D : 0.01D;
    }

    @Override
    public int approachNum() {
        return 0;
    }
}

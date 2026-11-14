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

package com.hbm.entity.missile;

import com.hbm.api.entity.RadarProvider;
import com.hbm.api.entity.RadarScanParams;
import com.hbm.api.entity.RadarTargetType;
import com.hbm.client.ClientEffects;
import com.hbm.entity.FarVisible;
import com.hbm.entity.missile.Missile;
import com.hbm.entity.projectile.InterpolatedProjectileEntity;
import com.hbm.explosion.BlastSmoke;
import com.hbm.explosion.CompositeExplosion;
import com.hbm.util.ChunkUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MissileAntiBallisticEntity extends InterpolatedProjectileEntity
        implements RadarProvider, FarVisible {

    public static double baseSpeed = 1.5D;

    public Entity tracking;
    public double velocity;
    protected int activationTimer;
    private EntityReference<Entity> savedTracking;

    public MissileAntiBallisticEntity(
            EntityType<? extends MissileAntiBallisticEntity> type, Level level) {
        super(type, level);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean isSpectral() {
        return activationTimer < 40;
    }

    @Override
    public boolean doesImpactEntities() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {

        var prePos = flightStart();
        super.tick();
        if (isRemoved()) return;

        if (!this.level().isClientSide()) {

            double oldSpeed = velocity;
            if (velocity < 6) velocity += 0.1;
            if (oldSpeed > 0) setDeltaMovement(getDeltaMovement().scale(velocity / oldSpeed));
            if (tracking == null) tracking = EntityReference.getEntity(savedTracking, level());

            if (this.activationTimer < 40) {
                this.activationTimer++;
                setDeltaMovement(0, baseSpeed * velocity, 0);
            } else {
                Entity prevTracking = this.tracking;

                if (this.tracking == null || !this.tracking.isAlive()) this.targetMissile();

                if (prevTracking == null && this.tracking != null) {
                    BlastSmoke.shock(this.level(), this.getX(), this.getY(), this.getZ(), 24, 3F);
                }
                if (this.tracking != null && this.tracking.isAlive()) {
                    this.aimAtTarget();
                } else {
                    if (this.tickCount > 600) this.discard();
                }
            }

            this.loadNeighboringChunks(
                    (int) Math.floor(this.getX() / 16D), (int) Math.floor(this.getZ() / 16D));

            if (this.getY() > 2000 && (this.tracking == null || !this.tracking.isAlive()))
                this.discard();

        } else {
            Vec3 step = this.position().subtract(prePos);
            if (step.lengthSqr() > 1.0e-8) {
                Vec3 vec = step.normalize();
                ClientEffects.spawnContrail(
                        this.level(),
                        this.getX() - vec.x,
                        this.getY() - vec.y,
                        this.getZ() - vec.z,
                        ClientEffects.Contrail.ABM);
            }
        }

        this.updateFlightRotation();
    }

    protected void targetMissile() {

        if (!(this.level() instanceof ServerLevel server)) return;

        Entity closest = null;
        double dist = 1_000;

        for (Entity e :
                ((com.hbm.interfaces.injected.ServerLevelExtension) (server)).hbm$radarTargets()) {
            if (!(e instanceof Missile missile)) continue;
            if (!missile.canBeSeenBy(null)) continue;

            Vec3 vec =
                    new Vec3(
                            e.getX() - this.getX(), e.getY() - this.getY(), e.getZ() - this.getZ());

            if (vec.length() < dist) {
                closest = e;
                dist = vec.length();
            }
        }

        this.tracking = closest;
    }

    protected void aimAtTarget() {

        Vec3 delta =
                new Vec3(
                        tracking.getX() - this.getX(),
                        tracking.getY() - this.getY(),
                        tracking.getZ() - this.getZ());
        double intercept = delta.length() / (baseSpeed * this.velocity);
        Vec3 predicted =
                new Vec3(
                        tracking.getX() + (tracking.getX() - tracking.xOld) * intercept,
                        tracking.getY() + (tracking.getY() - tracking.yOld) * intercept,
                        tracking.getZ() + (tracking.getZ() - tracking.zOld) * intercept);
        Vec3 motion =
                new Vec3(
                                predicted.x - this.getX(),
                                predicted.y - this.getY(),
                                predicted.z - this.getZ())
                        .normalize();

        if (delta.length() < 10 && this.activationTimer >= 40) {
            this.discard();
            CompositeExplosion.detonate(
                    this.level(),
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    15F,
                    CompositeExplosion.CLOUD);
        }

        setDeltaMovement(motion.scale(baseSpeed * velocity));
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (!this.level().isClientSide() && this.activationTimer >= 40) {
            this.discard();
            CompositeExplosion.detonate(
                    this.level(),
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    20F,
                    CompositeExplosion.CLOUD);
        }
    }

    @Override
    public double getGravityVelocity() {
        return 0.0D;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    public float getWaterDrag() {
        return 1F;
    }

    private void updateFlightRotation() {
        Vec3 motion = this.getDeltaMovement();
        float f2 = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI) - 90F;
        for (; pitch - this.xRotO < -180.0F; this.xRotO -= 360.0F)
            ;
        while (pitch - this.xRotO >= 180.0F) this.xRotO += 360.0F;
        while (yaw - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (yaw - this.yRotO >= 180.0F) this.yRotO += 360.0F;
        this.xRot = pitch;
        this.yRot = yaw;
    }

    @Override
    public String getRadarName() {
        return "radar.target.abm";
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.ANTI_BALLISTIC;
    }

    @Override
    public boolean canBeSeenBy(BlockEntity observer) {
        return true;
    }

    @Override
    public boolean matchesScan(RadarScanParams params) {
        return params.scanMissiles();
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        velocity = input.getDoubleOr("Velocity", 0);
        activationTimer = input.getIntOr("Activation", 0);
        savedTracking = EntityReference.read(input, "Tracking");
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("Velocity", velocity);
        output.putInt("Activation", activationTimer);
        EntityReference.store(
                tracking == null ? savedTracking : EntityReference.of(tracking),
                output,
                "Tracking");
    }

    public void loadNeighboringChunks(int newChunkX, int newChunkZ) {
        if (this.level() instanceof ServerLevel server) {
            ChunkUtil.holdForEntity(
                    server, new ChunkPos(newChunkX, newChunkZ), ChunkUtil.HOLD_RADIUS + 1);
        }
    }
}

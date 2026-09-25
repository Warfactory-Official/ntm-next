// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectable;
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.client.ClientEffects;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.util.ChunkUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityMissileAntiBallistic extends EntityThrowableInterp
        implements IRadarDetectable, IRadarDetectableNT {

    public static double baseSpeed = 1.5D;

    public Entity tracking;
    public double velocity;
    protected int activationTimer;

    public EntityMissileAntiBallistic(
            EntityType<? extends EntityMissileAntiBallistic> type, Level level) {
        super(type, level);

        this.setDeltaMovement(0.0D, baseSpeed, 0.0D);
    }

    @Override
    protected double motionMult() {
        return this.velocity;
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

        Vec3 prePos = this.position();
        super.tick();

        if (!this.level().isClientSide()) {

            if (this.velocity < 6) this.velocity += 0.1;

            if (this.activationTimer < 40) {
                this.activationTimer++;
                this.setDeltaMovement(
                        this.getDeltaMovement().x, baseSpeed, this.getDeltaMovement().z);
            } else {
                Entity prevTracking = this.tracking;

                if (this.tracking == null || !this.tracking.isAlive()) this.targetMissile();

                if (prevTracking == null && this.tracking != null) {
                    ExplosionLarge.spawnShock(
                            this.level(), this.getX(), this.getY(), this.getZ(), 24, 3F);
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

        for (Entity e : server.getAllEntities()) {
            if (!(e instanceof EntityMissileBaseNT)) continue;
            if (e instanceof EntityMissileStealth) continue;

            Vec3 vec =
                    new Vec3(
                            e.getX() - this.getX(), e.getY() - this.getY(), e.getZ() - this.getZ());

            if (vec.length() < dist) {
                closest = e;
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
            ExplosionLarge.explode(
                    this.level(), this.getX(), this.getY(), this.getZ(), 15F, true, false, false);
        }

        this.setDeltaMovement(motion.x * baseSpeed, motion.y * baseSpeed, motion.z * baseSpeed);
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (!this.level().isClientSide() && this.activationTimer >= 40) {
            this.discard();
            ExplosionLarge.explode(
                    this.level(), this.getX(), this.getY(), this.getZ(), 20F, true, false, false);
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
    protected float getWaterDrag() {
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
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_AB;
    }

    @Override
    public String getRadarName() {
        return "radar.target.abm";
    }

    @Override
    public int getBlipLevel() {
        return IRadarDetectableNT.TIER_AB;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanMissiles;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.velocity = input.getDoubleOr("veloc", this.velocity);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("veloc", this.velocity);
    }

    public void loadNeighboringChunks(int newChunkX, int newChunkZ) {
        if (this.level() instanceof ServerLevel server) {
            ChunkUtil.holdForEntity(
                    server, new ChunkPos(newChunkX, newChunkZ), ChunkUtil.HOLD_RADIUS + 1);
        }
    }
}

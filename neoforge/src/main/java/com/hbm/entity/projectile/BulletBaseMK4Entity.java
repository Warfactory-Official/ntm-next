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
import com.hbm.entity.projectile.InterpolatedProjectileEntity;
import com.hbm.items.weapon.sedna.BulletConfig.ProjectileType;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import com.hbm.util.ChunkUtil;
import com.hbm.util.NtmMath;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BulletBaseMK4Entity extends InterpolatedProjectileEntity {

    private static final EntityDataAccessor<Integer> CONFIG_ID =
            SynchedEntityData.defineId(BulletBaseMK4Entity.class, EntityDataSerializers.INT);
    public BulletConfig config;
    public double velocity;
    public double prevVelocity;
    public double accel;
    private Vec3 heading = Vec3.ZERO;
    public float damage;
    public int ricochets = 0;
    public Entity lockonTarget;
    private EntityReference<Entity> savedLockonTarget;
    public boolean ignoreFrustum;

    public BulletBaseMK4Entity(EntityType<? extends BulletBaseMK4Entity> type, Level level) {
        super(type, level);
    }

    public BulletBaseMK4Entity(
            Level level,
            LivingEntity entity,
            BulletConfig config,
            float damage,
            float gunSpread,
            double posX,
            double posY,
            double posZ,
            double motionX,
            double motionY,
            double motionZ) {
        this(ModEntities.BULLET_MK4.get(), level);

        this.setThrower(entity);
        this.setBulletConfig(config);

        this.damage = damage;

        this.snapTo(posX, posY, posZ, 0, 0);

        this.setDeltaMovement(motionX, motionY, motionZ);
        this.setThrowableHeading(motionX, motionY, motionZ, 1.0F, this.config.spread + gunSpread);
    }

    public BulletBaseMK4Entity(
            LivingEntity entity,
            BulletConfig config,
            float baseDamage,
            float gunSpread,
            double sideOffset,
            double heightOffset,
            double frontOffset) {
        this(ModEntities.BULLET_MK4.get(), entity.level());

        this.setThrower(entity);
        this.setBulletConfig(config);

        this.damage = baseDamage * this.config.damageMult;

        this.snapTo(
                entity.getX(),
                entity.getY() + entity.getEyeHeight(),
                entity.getZ(),
                entity.getYRot(),
                entity.getXRot());

        Vec3 offset =
                new Vec3(sideOffset, heightOffset, frontOffset)
                        .xRot(-this.getXRot() / 180F * (float) Math.PI)
                        .yRot(-this.getYRot() / 180F * (float) Math.PI);

        this.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);

        var direction = getLookAngle();
        setThrowableHeading(direction.x, direction.y, direction.z, 1.0F, gunSpread);
    }

    public BulletBaseMK4Entity(
            Level level,
            BulletConfig config,
            float baseDamage,
            float gunSpread,
            float yaw,
            float pitch) {
        this(ModEntities.BULLET_MK4.get(), level);

        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;

        float yawDeg = yaw * 180F / (float) Math.PI;
        float pitchDeg = -pitch * 180F / (float) Math.PI;
        this.setYRot(yawDeg);
        this.yRotO = yawDeg;
        this.setXRot(pitchDeg);
        this.xRotO = pitchDeg;

        var direction = getLookAngle();
        setThrowableHeading(direction.x, direction.y, direction.z, 1.0F, gunSpread);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CONFIG_ID, 0);
    }

    public BulletConfig getBulletConfig() {
        int id = this.entityData.get(CONFIG_ID);
        return BulletConfig.configs.get(id);
    }

    public void setBulletConfig(BulletConfig config) {
        this.config = config;
        this.entityData.set(CONFIG_ID, config.id);
    }

    @Override
    public void tick() {

        config = getBulletConfig();
        if (lockonTarget == null)
            lockonTarget = EntityReference.getEntity(savedLockonTarget, level());

        var previous = flightStart();

        super.tick();
        if (isRemoved()) return;

        double dX = getX() - previous.x;
        double dY = getY() - previous.y;
        double dZ = getZ() - previous.z;

        if (!this.inGround
                && this.lockonTarget != null
                && this.lockonTarget.isAlive()
                && !this.lockonTarget.isSpectator()) {
            Vec3 motion = getFlightDirection();
            double vel = motion.length();
            Vec3 delta =
                    new Vec3(
                            lockonTarget.getX() - getX(),
                            lockonTarget.getY() + lockonTarget.getBbHeight() / 2D - getY(),
                            lockonTarget.getZ() - getZ());
            float turn = Math.min(0.005F * this.tickCount, 1F);
            Vec3 newVec =
                    new Vec3(
                                    NtmMath.lerp(motion.x, delta.x, turn),
                                    NtmMath.lerp(motion.y, delta.y, turn),
                                    NtmMath.lerp(motion.z, delta.z, turn))
                            .normalize()
                            .scale(vel);
            setFlightDirection(newVec);
        }

        this.prevVelocity = this.velocity;
        this.velocity = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        if ((config != XFactoryTool.ct_hook || !level().isClientSide())
                && !inGround
                && !onGround()
                && velocity > 0) {
            float yaw = (float) (Mth.atan2(dX, dZ) * 180 / Math.PI);
            float pitch = (float) (Mth.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)) * 180 / Math.PI);
            yRotO = yaw - Mth.wrapDegrees(yaw - yRotO);
            xRotO = pitch - Mth.wrapDegrees(pitch - xRotO);
            setYRot(yaw);
            setXRot(pitch);
        }

        if (config.pType == ProjectileType.BULLET_CHUNKLOADING) ChunkUtil.holdOwnChunk(this);

        if (!level().isClientSide() && tickCount > config.expires) {
            discard();
            return;
        }

        if (this.config.onUpdate != null) this.config.onUpdate.accept(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);

        if (reason.shouldDestroy()) this.sendTeleport();
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (!this.level().isClientSide()) {

            if (this.config.onImpact != null) this.config.onImpact.accept(this, mop);
            if (this.isRemoved() || this.inGround) return;
            if (this.config.onRicochet != null) this.config.onRicochet.accept(this, mop);
            if (isRemoved() || inGround) return;
            if (this.config.onEntityHit != null) this.config.onEntityHit.accept(this, mop);
        }
    }

    @Override
    public Vec3 getMovementToShoot(double x, double y, double z, float power, float uncertainty) {
        return new Vec3(x, y, z)
                .normalize()
                .add(
                        random.nextGaussian() * uncertainty,
                        random.nextGaussian() * uncertainty,
                        random.nextGaussian() * uncertainty)
                .scale(power);
    }

    @Override
    public void setThrowableHeading(
            double x, double y, double z, float velocity, float inaccuracy) {
        shoot(x, y, z, velocity, inaccuracy);
        setFlightDirection(getDeltaMovement());
    }

    private double flightSpeed() {
        return config.velocity + accel;
    }

    public Vec3 getFlightDirection() {
        double speed = flightSpeed();
        if (speed != 0) heading = getDeltaMovement().scale(1 / speed);
        return heading;
    }

    public void setFlightDirection(Vec3 direction) {
        heading = direction;
        setDeltaMovement(direction.scale(flightSpeed()));
    }

    public void setFlightDirection(double x, double y, double z) {
        setFlightDirection(new Vec3(x, y, z));
    }

    public void setAcceleration(double acceleration) {
        var direction = getFlightDirection();
        accel = acceleration;
        setFlightDirection(direction);
    }

    @Override
    public double getGravityVelocity() {
        return config.gravity * flightSpeed();
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    public float getWaterDrag() {
        return 1F;
    }

    @Override
    public boolean doesImpactEntities() {
        return this.config.impactsEntities;
    }

    @Override
    public boolean doesPenetrate() {
        return this.config.doesPenetrate;
    }

    @Override
    public boolean isSpectral() {
        return this.config.isSpectral;
    }

    @Override
    public int selfDamageDelay() {
        return this.config.selfDamageDelay;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Config", getBulletConfig().id);
        output.putFloat("Damage", damage);
        output.putDouble("Acceleration", accel);
        output.store("Heading", Vec3.CODEC, getFlightDirection());
        output.putDouble("Velocity", velocity);
        output.putInt("Ricochets", ricochets);
        output.putBoolean("IgnoreFrustum", ignoreFrustum);
        EntityReference.store(
                lockonTarget == null ? savedLockonTarget : EntityReference.of(lockonTarget),
                output,
                "HomingTarget");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setBulletConfig(BulletConfig.configs.get(input.getIntOr("Config", getBulletConfig().id)));
        damage = input.getFloatOr("Damage", 0);
        accel = input.getDoubleOr("Acceleration", 0);
        input.read("Heading", Vec3.CODEC).ifPresent(direction -> heading = direction);
        velocity = input.getDoubleOr("Velocity", 0);
        prevVelocity = velocity;
        ricochets = input.getIntOr("Ricochets", 0);
        ignoreFrustum = input.getBooleanOr("IgnoreFrustum", false);
        savedLockonTarget = EntityReference.read(input, "HomingTarget");
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}

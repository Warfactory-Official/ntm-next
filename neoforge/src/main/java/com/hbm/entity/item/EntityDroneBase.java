// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class EntityDroneBase extends Entity {

    private static final EntityDataAccessor<Byte> APPEARANCE =
            SynchedEntityData.defineId(EntityDroneBase.class, EntityDataSerializers.BYTE);

    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    public double targetX = -1;
    public double targetY = -1;
    public double targetZ = -1;

    protected EntityDroneBase(EntityType<? extends EntityDroneBase> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(APPEARANCE, (byte) 0);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player) discard();
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    public void setAppearance(int style) {
        entityData.set(APPEARANCE, (byte) style);
    }

    public int getAppearance() {
        return entityData.get(APPEARANCE);
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) {
            for (Vec3 offset : ROTOR_OFFSETS) {
                level().addParticle(
                                ParticleTypes.SMOKE,
                                getX() + offset.x,
                                getY() + 0.75,
                                getZ() + offset.z,
                                0,
                                -0.2,
                                0);
            }
            return;
        }

        setDeltaMovement(Vec3.ZERO);

        if (this.targetY != -1) {
            Vec3 delta = new Vec3(targetX - getX(), targetY - getY(), targetZ - getZ());
            double speed = Math.min(getSpeed(), delta.length());
            setDeltaMovement(delta.normalize().scale(speed));
        }

        if (this.horizontalCollision) {
            setDeltaMovement(getDeltaMovement().add(0, 1, 0));
        }

        loadNeighboringChunks();
        move(MoverType.SELF, getDeltaMovement());
    }

    private static final Vec3[] ROTOR_OFFSETS = {
        new Vec3(1.125, 0, 0), new Vec3(-1.125, 0, 0),
        new Vec3(0, 0, 1.125), new Vec3(0, 0, -1.125)
    };

    protected void loadNeighboringChunks() {}

    public double getSpeed() {
        return 0.125D;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        if (input.getDoubleOr("tY", -1) != -1) {
            this.targetX = input.getDoubleOr("tX", -1);
            this.targetY = input.getDoubleOr("tY", -1);
            this.targetZ = input.getDoubleOr("tZ", -1);
        }
        entityData.set(APPEARANCE, (byte) input.getIntOr("app", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putDouble("tX", targetX);
        output.putDouble("tY", targetY);
        output.putDouble("tZ", targetZ);
        output.putInt("app", getAppearance());
    }
}

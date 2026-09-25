// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.client.ClientEffects;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.helper.ExplosionSmallCreator;
import com.hbm.sound.ModSounds;
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

public abstract class EntityPlaneBase extends Entity {

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(EntityPlaneBase.class, EntityDataSerializers.FLOAT);
    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    public float health = getMaxHealth();
    public int timer = getLifetime();

    public float renderPitch = -90F;
    public float renderPitchO = -90F;
    public float renderYaw;
    public float renderYawO;

    public EntityPlaneBase(EntityType<? extends EntityPlaneBase> type, Level level) {
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
                new ExplosionVNT(level(), x, y, z, 15F).makeStandard().explode();
                level().playSound(
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
        level().playSound(
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
        if (level() instanceof ServerLevel server) ChunkUtil.loadForEntity(server, chunkPosition());
    }
}

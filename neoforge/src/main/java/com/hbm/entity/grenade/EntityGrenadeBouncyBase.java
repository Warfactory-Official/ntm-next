// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.grenade;

import com.hbm.NuclearTech;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class EntityGrenadeBouncyBase extends Projectile {

    protected int timer = 0;

    public EntityGrenadeBouncyBase(
            EntityType<? extends EntityGrenadeBouncyBase> type, Level level) {
        super(type, level);
    }

    public void initThrower(LivingEntity thrower) {
        setOwner(thrower);
        snapTo(
                thrower.getX(),
                thrower.getY() + thrower.getEyeHeight(),
                thrower.getZ(),
                thrower.getYRot(),
                thrower.getXRot());
        double px = getX() - Math.cos(getYRot() / 180.0F * Math.PI) * 0.16F;
        double py = getY() - 0.10000000149011612D;
        double pz = getZ() - Math.sin(getYRot() / 180.0F * Math.PI) * 0.16F;
        setPos(px, py, pz);
        float velocity = 0.4F;
        double motionX =
                -Math.sin(getYRot() / 180.0F * Math.PI)
                        * Math.cos(getXRot() / 180.0F * Math.PI)
                        * velocity;
        double motionZ =
                Math.cos(getYRot() / 180.0F * Math.PI)
                        * Math.cos(getXRot() / 180.0F * Math.PI)
                        * velocity;
        double motionY = -Math.sin((getXRot() + throwAngle()) / 180.0F * Math.PI) * velocity;
        setThrowableHeading(motionX, motionY, motionZ, throwForce(), 1.0F);
        setXRot(0F);
        xRotO = 0F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        double perimeter = getBoundingBox().getSize() * 4.0D;
        perimeter *= 64.0D;
        return dist < perimeter * perimeter;
    }

    protected float throwForce() {
        return 1.5F;
    }

    protected float throwAngle() {
        return 0.0F;
    }

    protected double getGravityVelocity() {
        return 0.03D;
    }

    public void setThrowableHeading(
            double motionX, double motionY, double motionZ, float velocity, float inaccuracy) {
        float len = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= len;
        motionY /= len;
        motionZ /= len;
        motionX += random.nextGaussian() * 0.007499999832361937D * inaccuracy;
        motionY += random.nextGaussian() * 0.007499999832361937D * inaccuracy;
        motionZ += random.nextGaussian() * 0.007499999832361937D * inaccuracy;
        motionX *= velocity;
        motionY *= velocity;
        motionZ *= velocity;
        setDeltaMovement(motionX, motionY, motionZ);
        float yaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
        setYRot(yaw);
        yRotO = yaw;
    }

    @Override
    public void lerpMotion(Vec3 movement) {
        setDeltaMovement(movement);

        if (xRotO == 0.0F && yRotO == 0.0F) {
            float yaw = (float) (Math.atan2(movement.x, movement.z) * 180.0D / Math.PI);
            setYRot(yaw);
            yRotO = yaw;
        }
    }

    @Override
    public void tick() {
        super.tick();

        xRotO = getXRot();

        setXRot(getXRot() - (float) (getDeltaMovement().length() * 25));

        double px = getX();
        double py = getY();
        double pz = getZ();
        Vec3 motion = getDeltaMovement();
        boolean bounce = false;

        Vec3 from = new Vec3(px, py, pz);
        Vec3 to = from.add(motion);
        BlockHitResult hit =
                level().clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this));

        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 hitVec = hit.getLocation();
            px += (hitVec.x - px) * 0.6;
            py += (hitVec.y - py) * 0.6;
            pz += (hitVec.z - pz) * 0.6;

            Direction side = hit.getDirection();
            motion =
                    switch (side.getAxis()) {
                        case Y -> motion.multiply(1, -1, 1);
                        case Z -> motion.multiply(1, 1, -1);
                        case X -> motion.multiply(-1, 1, 1);
                    };

            bounce = true;

            if (motion.length() > 0.05) {
                level().playSound(
                                null,
                                px,
                                py,
                                pz,
                                ModSounds.G_BOUNCE.get(),
                                SoundSource.PLAYERS,
                                2.0F,
                                1.0F);
            }

            motion = motion.scale(getBounceMod());
        }

        if (!bounce) {
            px += motion.x;
            py += motion.y;
            pz += motion.z;
        }

        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);

        while (yaw - yRotO < -180.0F) yRotO -= 360.0F;
        while (yaw - yRotO >= 180.0F) yRotO += 360.0F;

        setYRot(yRotO + (yaw - yRotO) * 0.2F);
        float drag = 0.99F;
        double gravity = getGravityVelocity();

        if (isInWater()) {
            for (int i = 0; i < 4; ++i) {
                float f = 0.25F;
                level().addParticle(
                                ParticleTypes.BUBBLE,
                                px - motion.x * f,
                                py - motion.y * f,
                                pz - motion.z * f,
                                motion.x,
                                motion.y,
                                motion.z);
            }

            drag = 0.8F;
        }

        setDeltaMovement(motion.scale(drag).subtract(0, gravity, 0));
        setPos(px, py, pz);

        timer++;

        if (timer >= getMaxTimer() && !level().isClientSide()) {
            explode();

            if (Services.CONFIG.runtime().extendedLogging()) {
                Entity thrower = getOwner();
                NuclearTech.LOGGER.info(
                        "[GREN] Set off grenade at {} / {} / {} by {}!",
                        (int) getX(),
                        (int) getY(),
                        (int) getZ(),
                        thrower instanceof Player player
                                ? player.getDisplayName().getString()
                                : "null");
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        timer = input.getIntOr("timer", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("timer", timer);
    }

    public LivingEntity getThrower() {
        return getOwner() instanceof LivingEntity living ? living : null;
    }

    public abstract void explode();

    protected abstract int getMaxTimer();

    protected abstract double getBounceMod();
}

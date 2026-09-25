// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;

public abstract class EntityThrowableNT extends ThrowableProjectile {

    private static final EntityDataAccessor<Byte> STUCK_IN =
            SynchedEntityData.defineId(EntityThrowableNT.class, EntityDataSerializers.BYTE);
    public int throwableShake;
    public int ticksInGround;
    public int ticksInAir;
    protected boolean inGround;
    private BlockPos stuckBlockPos = BlockPos.ZERO;

    private Block stuckBlock;

    public EntityThrowableNT(EntityType<? extends EntityThrowableNT> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STUCK_IN, (byte) 0);
    }

    public int getStuckIn() {
        return this.entityData.get(STUCK_IN);
    }

    public void setStuckIn(int side) {
        this.entityData.set(STUCK_IN, (byte) side);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        double perimeter = this.getBoundingBox().getSize() * 4.0D;
        perimeter *= 64.0D;
        return dist < perimeter * perimeter;
    }

    protected float throwForce() {
        return 1.5F;
    }

    protected double headingForceMult() {
        return 0.0075D;
    }

    protected float throwAngle() {
        return 0.0F;
    }

    protected double motionMult() {
        return 1.0D;
    }

    public void initThrower(LivingEntity thrower) {
        setThrower(thrower);
        snapTo(
                thrower.getX(),
                thrower.getY() + thrower.getEyeHeight(),
                thrower.getZ(),
                thrower.getYRot(),
                thrower.getXRot());
        double px = getX() - Math.cos(getYRot() / 180.0F * Math.PI) * 0.16F;
        double py = getY() - 0.1D;
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
        double motionY = -Math.sin((getXRot() + this.throwAngle()) / 180.0F * Math.PI) * velocity;
        this.setThrowableHeading(motionX, motionY, motionZ, this.throwForce(), 1.0F);
    }

    public void setThrowableHeading(
            double motionX, double motionY, double motionZ, float velocity, float inaccuracy) {
        float throwLen =
                (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= throwLen;
        motionY /= throwLen;
        motionZ /= throwLen;
        motionX += this.random.nextGaussian() * headingForceMult() * inaccuracy;
        motionY += this.random.nextGaussian() * headingForceMult() * inaccuracy;
        motionZ += this.random.nextGaussian() * headingForceMult() * inaccuracy;
        motionX *= velocity;
        motionY *= velocity;
        motionZ *= velocity;
        this.setDeltaMovement(motionX, motionY, motionZ);
        double hyp = Math.sqrt(motionX * motionX + motionZ * motionZ);
        float yaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motionY, hyp) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(pitch);
        this.xRotO = pitch;
        this.ticksInGround = 0;
    }

    @Override
    public void tick() {
        this.baseTick();

        if (this.throwableShake > 0) {
            --this.throwableShake;
        }

        if (this.inGround) {
            if (this.level().getBlockState(this.stuckBlockPos).getBlock() == this.stuckBlock) {
                ++this.ticksInGround;

                if (this.groundDespawn() > 0 && this.ticksInGround == this.groundDespawn()) {
                    this.discard();
                }

            } else {

                this.inGround = false;
                this.setDeltaMovement(
                        this.getDeltaMovement()
                                .multiply(
                                        this.random.nextFloat() * 0.2F,
                                        this.random.nextFloat() * 0.2F,
                                        this.random.nextFloat() * 0.2F));
                this.ticksInGround = 0;
                this.ticksInAir = 0;
            }

        } else {
            ++this.ticksInAir;

            Vec3 motion = this.getDeltaMovement().scale(motionMult());
            Vec3 pos = this.position();
            Vec3 nextPos = pos.add(motion);
            HitResult mop = null;
            if (!this.isSpectral()) {
                BlockHitResult blockHit =
                        this.level()
                                .clip(
                                        new ClipContext(
                                                pos,
                                                nextPos,
                                                ClipContext.Block.COLLIDER,
                                                ClipContext.Fluid.NONE,
                                                this));
                if (blockHit.getType() != HitResult.Type.MISS) mop = blockHit;
            }

            if (mop != null) {
                nextPos = mop.getLocation();
            }

            if (!this.level().isClientSide() && this.doesImpactEntities()) {

                Entity hitEntity = null;
                List<Entity> list =
                        this.level()
                                .getEntities(
                                        this,
                                        this.getBoundingBox()
                                                .expandTowards(motion)
                                                .inflate(1.0D, 1.0D, 1.0D));
                double nearest = 0.0D;
                LivingEntity thrower = this.getThrower();
                Vec3 nonPenImpact = null;

                for (int j = 0; j < list.size(); ++j) {
                    Entity entity = list.get(j);

                    if (entity.isPickable()
                            && (entity != thrower || this.ticksInAir >= this.selfDamageDelay())
                            && entity.isAlive()) {
                        double hitbox = 0.3F;
                        AABB aabb = entity.getBoundingBox().inflate(hitbox, hitbox, hitbox);
                        Vec3 hitVec = aabb.clip(pos, nextPos).orElse(null);

                        if (hitVec != null) {

                            if (this.doesPenetrate()) {
                                this.onImpact(new EntityHitResult(entity, hitVec));
                            } else {

                                double dist = pos.distanceTo(hitVec);

                                if (dist < nearest || nearest == 0.0D) {
                                    hitEntity = entity;
                                    nearest = dist;
                                    nonPenImpact = hitVec;
                                }
                            }
                        }
                    }
                }

                if (!this.doesPenetrate() && hitEntity != null) {
                    mop = new EntityHitResult(hitEntity, nonPenImpact);
                }
            }

            if (mop != null) {

                this.onImpact(mop);
            }

            if (!this.onGround()) {
                Vec3 vel = this.getDeltaMovement();
                double hyp = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                float yaw = (float) (Math.atan2(vel.x, vel.z) * 180.0D / Math.PI);
                float pitch = (float) (Math.atan2(vel.y, hyp) * 180.0D / Math.PI);

                for (; pitch - this.xRotO < -180.0F; this.xRotO -= 360.0F)
                    ;
                while (pitch - this.xRotO >= 180.0F) this.xRotO += 360.0F;
                while (yaw - this.yRotO < -180.0F) this.yRotO -= 360.0F;
                while (yaw - this.yRotO >= 180.0F) this.yRotO += 360.0F;

                this.setXRot(this.xRotO + (pitch - this.xRotO) * 0.2F);
                this.setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);
            }

            float drag = this.getAirDrag();
            double gravity = this.getGravityVelocity();

            if (fullBlockCollisions()) {
                this.move(MoverType.SELF, motion);
            } else {
                this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            }

            if (this.isInWater()) {
                Vec3 vel = this.getDeltaMovement();
                for (int i = 0; i < 4; ++i) {
                    float f = 0.25F;
                    this.level()
                            .addParticle(
                                    ParticleTypes.BUBBLE,
                                    this.getX() - vel.x * f,
                                    this.getY() - vel.y * f,
                                    this.getZ() - vel.z * f,
                                    vel.x,
                                    vel.y,
                                    vel.z);
                }

                drag = this.getWaterDrag();
            }

            this.setDeltaMovement(this.getDeltaMovement().scale(drag).subtract(0, gravity, 0));
        }
    }

    public boolean fullBlockCollisions() {
        return false;
    }

    public boolean doesImpactEntities() {
        return true;
    }

    public boolean doesPenetrate() {
        return false;
    }

    public boolean isSpectral() {
        return false;
    }

    public int selfDamageDelay() {
        return 5;
    }

    public void getStuck(BlockPos pos, int side) {
        this.stuckBlockPos = pos;
        this.stuckBlock = level().getBlockState(pos).getBlock();
        this.inGround = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setStuckIn(side);
        this.sendTeleport();
    }

    public double getGravityVelocity() {
        return 0.03D;
    }

    protected abstract void onImpact(HitResult mop);

    @Override
    protected void onHit(HitResult result) {
        this.onImpact(result);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("xTile", this.stuckBlockPos.getX());
        output.putInt("yTile", this.stuckBlockPos.getY());
        output.putInt("zTile", this.stuckBlockPos.getZ());
        output.storeNullable("inTile", BuiltInRegistries.BLOCK.byNameCodec(), this.stuckBlock);
        output.putByte("shake", (byte) this.throwableShake);
        output.putBoolean("inGround", this.inGround);
        output.putInt("ticksInGround", this.ticksInGround);
        output.putInt("ticksInAir", this.ticksInAir);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.stuckBlockPos =
                new BlockPos(
                        input.getIntOr("xTile", 0),
                        input.getIntOr("yTile", 0),
                        input.getIntOr("zTile", 0));
        this.stuckBlock = input.read("inTile", BuiltInRegistries.BLOCK.byNameCodec()).orElse(null);
        this.throwableShake = input.getByteOr("shake", (byte) 0) & 255;
        this.inGround = input.getBooleanOr("inGround", false);
        this.ticksInGround = input.getIntOr("ticksInGround", 0);
        this.ticksInAir = input.getIntOr("ticksInAir", 0);
    }

    public LivingEntity getThrower() {
        return this.getOwner() instanceof LivingEntity living ? living : null;
    }

    public void setThrower(LivingEntity thrower) {
        this.setOwner(thrower);
    }

    public void sendTeleport() {
        if (this.level() instanceof ServerLevel server) {
            server.getChunkSource()
                    .chunkMap
                    .sendToTrackingPlayers(this, ClientboundEntityPositionSyncPacket.of(this));
        }
    }

    @Override
    protected float getAirDrag() {
        return 0.99F;
    }

    protected float getWaterDrag() {
        return 0.8F;
    }

    protected int groundDespawn() {
        return 1200;
    }
}

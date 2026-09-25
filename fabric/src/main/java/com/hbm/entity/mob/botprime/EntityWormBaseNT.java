// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.botprime;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

public abstract class EntityWormBaseNT extends PathfinderMob {

    public int aggroCooldown;
    public int courseChangeCooldown;
    public double waypointX;
    public double waypointY;
    public double waypointZ;

    protected @Nullable Entity targetedEntity;
    protected @Nullable LivingEntity followed;
    protected boolean canFly;
    protected int dmgCooldown;
    protected boolean wasNearGround;
    protected BlockPos spawnPoint = BlockPos.ZERO;
    protected double attackRange;
    protected double maxSpeed;
    protected double fallSpeed;
    protected double rangeForParts;
    protected int surfaceY = 60;
    protected boolean didCheck;
    protected double maxBodySpeed;
    protected double segmentDistance;
    protected double knockbackDivider;

    protected float dragInAir;
    protected float dragInGround;

    private int headID;
    private int partNum;

    protected EntityWormBaseNT(EntityType<? extends EntityWormBaseNT> type, Level level) {
        super(type, level);
    }

    public int getPartNumber() {
        return this.partNum;
    }

    public void setPartNumber(int num) {
        this.partNum = num;
    }

    public @Nullable Entity getHead() {
        return level().getEntity(this.headID);
    }

    public int getHeadID() {
        return this.headID;
    }

    public void setHeadID(int id) {
        this.headID = id;
    }

    public boolean getIsHead() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (isInvulnerableToBase(source)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING)) {
            return false;
        }

        if (source.getEntity() instanceof EntityWormBaseNT worm && worm.getHeadID() == getHeadID())
            return false;

        markHurt();

        if (getIsHead()) return super.hurtServer(level, source, amount);

        Entity ahead = this.targetedEntity;
        if (ahead != null) return ahead.hurtServer(level, source, amount);

        return super.hurtServer(level, source, amount);
    }

    protected void updateActionState() {

        if (!level().isClientSide() && level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (this.targetedEntity != null && !this.targetedEntity.isAlive())
            this.targetedEntity = null;

        Vec3 motion = getDeltaMovement();
        if (getY() < -10D) {
            setDeltaMovement(motion.x, 1D, motion.z);
        } else if (getY() < 3D) {
            setDeltaMovement(motion.x, 0.3D, motion.z);
        }

        if (this.tickCount % 5 == 0) {
            attackEntitiesInList(level().getEntities(this, getBoundingBox().inflate(0.5D)));
        }
    }

    protected void attackEntitiesInList(List<Entity> targets) {
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity)) continue;
            if (target instanceof EntityWormBaseNT worm && worm.getHeadID() == getHeadID())
                continue;
            attackAsWorm(target);
        }
    }

    protected boolean attackAsWorm(Entity target) {

        if (!(level() instanceof ServerLevel server)) return false;
        if (!target.hurtServer(server, damageSources().mobAttack(this), getAttackStrength(target)))
            return false;

        double tx = (getBoundingBox().minX + getBoundingBox().maxX) / 2D;
        double ty = (getBoundingBox().minY + getBoundingBox().maxY) / 2D;
        double tz = (getBoundingBox().minZ + getBoundingBox().maxZ) / 2D;
        double dx = target.getX() - tx;
        double dy = target.getY() - ty;
        double dz = target.getZ() - tz;
        double knockback = this.knockbackDivider * (dx * dx + dy * dy + dz * dz + 0.1D);

        target.push(dx / knockback, dy / knockback, dz / knockback);
        return true;
    }

    public abstract float getAttackStrength(Entity target);

    @Override
    public void push(double x, double y, double z) {}

    protected boolean isCourseTraversable() {
        return this.canFly || isBuried();
    }

    protected boolean isBuried() {

        float width = getBbWidth() * 0.8F;
        AABB eyeBox = AABB.ofSize(getEyePosition(), width, 1.0E-6D, width);

        return BlockPos.betweenClosedStream(eyeBox)
                .anyMatch(
                        pos -> {
                            BlockState state = level().getBlockState(pos);
                            return !state.isAir()
                                    && state.isSuffocating(level(), pos)
                                    && Shapes.joinIsNotEmpty(
                                            state.getCollisionShape(level(), pos).move(pos),
                                            Shapes.create(eyeBox),
                                            BooleanOp.AND);
                        });
    }

    @Override
    public void travel(Vec3 travelVector) {

        float drag = isBuried() || isInWater() || isInLava() ? this.dragInGround : this.dragInAir;
        if (!getIsHead()) drag *= 0.9F;

        moveRelative(0.02F, travelVector);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(drag));
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {}

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    @Override
    public void remove(RemovalReason reason) {
        SoundEvent sound = getDeathSound();

        if (sound != null && level() instanceof ServerLevel server) {
            server.playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    sound,
                    getSoundSource(),
                    getSoundVolume(),
                    getVoicePitch());
        }

        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("wormID", getHeadID());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setHeadID(input.getIntOr("wormID", 0));
    }

    protected void updateMovement() {

        double targetingRange = 128D;

        if (this.targetedEntity != null
                && this.targetedEntity.distanceToSqr(this) < targetingRange * targetingRange) {
            this.waypointX = this.targetedEntity.getX();
            this.waypointY = this.targetedEntity.getY();
            this.waypointZ = this.targetedEntity.getZ();
        }

        if ((this.tickCount % 60 == 0 || this.tickCount == 1)
                && (this.targetedEntity == null || this.followed == null)) {
            findEntityToFollow(
                    level().getEntitiesOfClass(
                                    EntityWormBaseNT.class,
                                    getBoundingBox().inflate(this.rangeForParts)));
        }

        double dx = this.waypointX - getX();
        double dy = this.waypointY - getY();
        double dz = this.waypointZ - getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double speed = Math.max(0D, Math.min(dist - this.segmentDistance, this.maxBodySpeed));

        if (dist < this.segmentDistance * 0.895D) {
            setDeltaMovement(getDeltaMovement().scale(0.8D));
        } else {
            setDeltaMovement(dx / dist * speed, dy / dist * speed, dz / dist * speed);
        }
    }

    protected void findEntityToFollow(List<EntityWormBaseNT> segments) {

        for (EntityWormBaseNT segment : segments) {

            if (segment.getHeadID() != getHeadID()) continue;

            if (segment.getIsHead()) {
                if (getPartNumber() == 0) this.targetedEntity = segment;
                this.followed = segment;
            } else if (segment.getPartNumber() == getPartNumber() - 1) {
                this.targetedEntity = segment;
            }
        }

        this.didCheck = true;
    }
}

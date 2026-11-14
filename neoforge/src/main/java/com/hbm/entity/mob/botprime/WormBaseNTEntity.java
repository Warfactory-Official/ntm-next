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

package com.hbm.entity.mob.botprime;

import com.hbm.lib.ModifiedDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class WormBaseNTEntity extends PathfinderMob {

    private static final double LEADER_RANGE_SQ = 128D * 128D;
    private static final int RELINK_PERIOD = 60;

    protected @Nullable Entity targetedEntity;
    protected @Nullable LivingEntity followed;
    protected boolean didCheck;

    protected double waypointX;
    protected double waypointY;
    protected double waypointZ;

    protected double rangeForParts;
    protected double maxBodySpeed;
    protected double segmentDistance;
    protected double knockbackDivider;
    protected float dragInAir;
    protected float dragInGround;

    private int headID;
    private int partNum;
    private boolean decaying;
    private boolean enlisted;

    private boolean buried;
    private int buriedTick = -1;
    private @Nullable Vec3 buriedAt;

    protected WormBaseNTEntity(EntityType<? extends WormBaseNTEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return super.shouldRenderAtSqrDistance(distance / (15D * 15D));
    }

    public int getPartNumber() {
        return partNum;
    }

    public void setPartNumber(int num) {
        regroup(() -> partNum = num);
    }

    public int getHeadID() {
        return headID;
    }

    public void setHeadID(int id) {
        regroup(() -> headID = id);
    }

    public boolean getIsHead() {
        return false;
    }

    protected final boolean isKin(@Nullable Entity entity) {
        return entity instanceof WormBaseNTEntity segment && segment.headID == headID;
    }

    private void regroup(Runnable change) {
        if (!enlisted) {
            change.run();
            return;
        }
        var level = (ServerLevel) level();
        WormTypes.discharge(level, this);
        change.run();
        WormTypes.enlist(level, this);
    }

    @Override
    public void setLevelCallback(EntityInLevelCallback callback) {
        super.setLevelCallback(callback);
        if (!(level() instanceof ServerLevel level)) return;
        boolean present = callback != EntityInLevelCallback.NULL;
        if (present == enlisted) return;
        enlisted = present;
        if (present) {
            WormTypes.enlist(level, this);
        } else {
            WormTypes.discharge(level, this);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableToBase(source)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING)
                || isKin(source.getEntity())) {
            return false;
        }
        markHurt();
        var proxy = getIsHead() ? null : targetedEntity;
        return proxy == null
                ? super.hurtServer(level, source, amount)
                : proxy.hurtServer(level, source, amount);
    }

    protected final void decay(ServerLevel level, float amount) {
        decaying = true;
        try {
            super.hurtServer(
                    level,
                    ModifiedDamageSource.bypassingCooldown(damageSources().genericKill()),
                    amount);
        } finally {
            decaying = false;
        }
    }

    protected final boolean isDecaying() {
        return decaying;
    }

    protected void updateActionState(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) discard();

        var target = targetedEntity;
        if (target != null && (target.isRemoved() || target.isSpectator())) targetedEntity = null;

        double y = getY();
        if (y < 3D) {
            var motion = getDeltaMovement();
            setDeltaMovement(motion.x, y < -10D ? 1D : 0.3D, motion.z);
        }

        if (tickCount % 5 == 0) {
            for (var victim :
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            getBoundingBox().inflate(0.5D),
                            entity -> !entity.isSpectator() && !isKin(entity))) {
                strike(level, victim);
            }
        }
    }

    private void strike(ServerLevel level, LivingEntity victim) {
        if (!victim.hurtServer(level, damageSources().mobAttack(this), getAttackStrength(victim)))
            return;
        var box = getBoundingBox();
        double dx = victim.getX() - (box.minX + box.maxX) / 2D;
        double dy = victim.getY() - (box.minY + box.maxY) / 2D;
        double dz = victim.getZ() - (box.minZ + box.maxZ) / 2D;
        double falloff = knockbackDivider * (dx * dx + dz * dz + dy * dy + 0.1D);
        victim.push(dx / falloff, dy / falloff, dz / falloff);
    }

    public abstract float getAttackStrength(Entity target);

    @Override
    public void push(double x, double y, double z) {}

    protected void updateMovement(ServerLevel level) {
        var leader = targetedEntity;
        if (leader != null && leader.distanceToSqr(this) < LEADER_RANGE_SQ) {
            waypointX = leader.getX();
            waypointY = leader.getY();
            waypointZ = leader.getZ();
        }

        if ((tickCount % RELINK_PERIOD == 0 || tickCount == 1)
                && (targetedEntity == null || followed == null)) {
            relink(level);
        }

        double dx = waypointX - getX();
        double dy = waypointY - getY();
        double dz = waypointZ - getZ();
        double gap = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (gap < segmentDistance * 0.895D) {
            setDeltaMovement(getDeltaMovement().scale(0.8D));
            return;
        }
        double speed = Mth.clamp(gap - segmentDistance, 0D, maxBodySpeed);
        setDeltaMovement(dx / gap * speed, dy / gap * speed, dz / gap * speed);
    }

    private void relink(ServerLevel level) {
        var reach = getBoundingBox().inflate(rangeForParts);
        var head = WormTypes.head(level, headID, reach);
        if (head != null) {
            followed = head;
            if (partNum == 0) targetedEntity = head;
        }
        var ahead = WormTypes.part(level, headID, partNum - 1, reach);
        if (ahead != null) targetedEntity = ahead;
        didCheck = true;
    }

    protected final void faceAlong(double dx, double dy, double dz) {
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dx, dz) * 180D / Math.PI);
        float pitch = (float) (Math.atan2(dy, horizontal) * 180D / Math.PI);
        setYRot(yaw);
        yRotO = yaw;
        setXRot(pitch);
        xRotO = pitch;
    }

    protected final boolean isBuried() {
        var at = position();
        if (buriedTick != tickCount || buriedAt != at) {
            buried = probeBuried();
            buriedTick = tickCount;
            buriedAt = at;
        }
        return buried;
    }

    private boolean probeBuried() {
        float width = getBbWidth() * 0.8F;
        var slab = AABB.ofSize(getEyePosition(), width, 1.0E-6D, width);
        VoxelShape slabShape = null;
        for (var pos : BlockPos.betweenClosed(slab)) {
            BlockState state = level().getBlockState(pos);
            if (state.isAir() || !state.isSuffocating(level(), pos)) continue;
            if (slabShape == null) slabShape = Shapes.create(slab);
            if (Shapes.joinIsNotEmpty(
                    state.getCollisionShape(level(), pos).move(pos), slabShape, BooleanOp.AND)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void travel(Vec3 travelVector) {
        float drag = isInWater() || isInLava() || isBuried() ? dragInGround : dragInAir;
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
        var sound = getDeathSound();
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
        output.putInt("wormID", headID);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setHeadID(input.getIntOr("wormID", 0));
    }
}

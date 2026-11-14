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
import com.hbm.client.MissileExhaustEffects;
import com.hbm.entity.FarVisible;
import com.hbm.entity.projectile.ProjectilePath;
import com.hbm.explosion.BlastDebris;
import com.hbm.explosion.CompositeExplosion;
import com.hbm.items.weapon.MissileItem;
import com.hbm.util.ChunkUtil;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class Missile extends Projectile implements RadarProvider, FarVisible {
    public static final double EXHAUST_RANGE = 350D;

    private static final EntityDataAccessor<Byte> PAD_FACING =
            SynchedEntityData.defineId(Missile.class, EntityDataSerializers.BYTE);
    private static final byte UNSET_FACING = 5;
    private static final String[] TIER_NAMES = {
        "radar.target.tier0",
        "radar.target.tier1",
        "radar.target.tier2",
        "radar.target.tier3",
        "radar.target.tier4"
    };
    private static final RadarTargetType[] TIER_BLIPS = {
        RadarTargetType.TIER0,
        RadarTargetType.TIER1,
        RadarTargetType.TIER2,
        RadarTargetType.TIER3,
        RadarTargetType.TIER4
    };

    public int targetX;
    public int targetZ;
    public double speed;
    private int startX;
    private int startZ;
    private double horizontalGain;
    private double verticalGain;
    private int health = 50;
    protected boolean burstsInAir;

    protected float viewYaw;
    protected float viewPitch;
    private float viewYawO;
    private float viewPitchO;

    protected Missile(EntityType<? extends Missile> type, Level level) {
        super(type, level);
    }

    public static boolean canReach(double x, double z, int targetX, int targetZ) {
        return (int) x != targetX || (int) z != targetZ;
    }

    public static int puffCount(double length) {
        return (int) Math.ceil(Math.max(Math.min(length, 10D), 1D));
    }

    public static boolean withinExhaustRange(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz <= EXHAUST_RANGE * EXHAUST_RANGE;
    }

    public abstract Item missileItem();

    public abstract void detonate(@Nullable BlockHitResult contact);

    public List<ItemStack> debris() {
        return List.of();
    }

    public ItemStack rareDebris() {
        return ItemStack.EMPTY;
    }

    public void launch(double x, double y, double z, int targetX, int targetZ) {
        if (!canReach(x, z, targetX, targetZ)) {
            throw new IllegalArgumentException(
                    "a missile at " + x + ", " + z + " has no programme to its own column");
        }
        setPos(x, y, z);
        startX = (int) x;
        startZ = (int) z;
        this.targetX = targetX;
        this.targetZ = targetZ;
        setDeltaMovement(0D, 2D, 0D);
        double dx = targetX - startX;
        double dz = targetZ - startZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        horizontalGain = 1D / length;
        verticalGain = 2D / length;
        speed = 0D;
        setYRot((float) (Math.atan2(targetX - x, targetZ - z) * 180D / Math.PI));
    }

    public String sourceName() {
        return getClass().getSimpleName();
    }

    public byte padFacing() {
        return entityData.get(PAD_FACING);
    }

    public void setPadFacing(Direction facing) {
        entityData.set(PAD_FACING, (byte) facing.get3DDataValue());
    }

    public float viewYaw(float partialTick) {
        return Mth.rotLerp(partialTick, viewYawO, viewYaw);
    }

    public float viewPitch(float partialTick) {
        return Mth.lerp(partialTick, viewPitchO, viewPitch);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PAD_FACING, UNSET_FACING);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this);
    }

    @Override
    public MoveSimulationType getMoveSimulationType() {
        return MoveSimulationType.AUTHORITATIVE_SIDE;
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            var from = oldPosition();
            double fromX = from.x, fromY = from.y, fromZ = from.z;
            super.tick();
            accelerate();
            present(getX() - fromX, getY() - fromY, getZ() - fromZ);
            return;
        }
        super.tick();
        if (!isRemoved()) fly();
    }

    protected void fly() {
        var motion = getDeltaMovement();
        double mx = motion.x, my = motion.y, mz = motion.z;
        if (!moveFlight(mx * speed, my * speed, mz * speed)) return;
        accelerate();
        double nx, ny, nz;
        if (hasPropulsion()) {
            ny = my - verticalGain * speed;
            double ux = targetX - startX, uz = targetZ - startZ;
            double length = Math.sqrt(ux * ux + uz * uz);
            if (length < 1.0E-5F) {
                ux = 0D;
                uz = 0D;
            } else {
                ux /= length;
                uz /= length;
            }
            nx = mx;
            nz = mz;
            if (ny > 0D) {
                nx += ux * horizontalGain * speed;
                nz += uz * horizontalGain * speed;
            } else if (ny < 0D) {
                nx -= ux * horizontalGain * speed;
                nz -= uz * horizontalGain * speed;
            }
        } else {
            nx = mx * 0.99D;
            nz = mz * 0.99D;
            ny = my > -1.5D ? my - 0.05D : my;
        }
        setDeltaMovement(nx, ny, nz);
        if (burstsInAir && ny < -1.5D) {
            float horizontal = (float) Math.sqrt(mx * mx + mz * mz);
            xRot = lerpRotation(getXRot(), (float) (Math.atan2(my, horizontal) * 180D / Math.PI));
            setYRot(lerpRotation(getYRot(), (float) (Math.atan2(mx, mz) * 180D / Math.PI)));
            detonate(null);
            discard();
            return;
        }
        setYRot((float) (Math.atan2(targetX - getX(), targetZ - getZ()) * 180D / Math.PI));
        float horizontal = (float) Math.sqrt(nx * nx + nz * nz);
        xRot = (float) (Math.atan2(ny, horizontal) * 180D / Math.PI) - 90F;
        ChunkUtil.holdOwnChunk(this);
    }

    private void accelerate() {
        if (speed < 4D) speed += Math.min(tickCount / 1200D, 0.05D);
    }

    protected boolean hasPropulsion() {
        return true;
    }

    protected boolean shouldHitBlocks() {
        return true;
    }

    protected final boolean moveFlight(Vec3 step) {
        return moveFlight(step.x, step.y, step.z);
    }

    private boolean moveFlight(double dx, double dy, double dz) {
        var from = position();
        var to = new Vec3(from.x + dx, from.y + dy, from.z + dz);
        if (shouldHitBlocks()) {
            var hit =
                    level().clip(
                                    new ClipContext(
                                            from,
                                            to,
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            this));
            if (hit.getType() != HitResult.Type.MISS && ProjectilePath.impact(this, hit) != null)
                return false;
        }
        setPos(to);
        applyEffectsFromBlocks();
        return true;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        onFlightImpact(hit);
    }

    protected void onFlightImpact(BlockHitResult hit) {
        discard();
        detonate(hit);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableToBase(source)) return false;
        if (health > 0) {
            health = (int) (health - amount);
            if (health <= 0) killMissile();
        }
        return true;
    }

    protected void killMissile() {
        if (isRemoved()) return;
        double x = getX(), y = getY(), z = getZ();
        var motion = getDeltaMovement();
        discard();
        var level = level();
        CompositeExplosion.detonate(
                level, x, y, z, 5F, CompositeExplosion.CLOUD | CompositeExplosion.SHRAPNEL);
        BlastDebris.shower(level, x, y, z, motion.x, motion.y, motion.z, 15, 0.075D);
        BlastDebris.missileDebris(
                level, x, y, z, motion.x, motion.y, motion.z, 0.25D, debris(), rareDebris());
    }

    private void present(double sx, double sy, double sz) {
        viewYawO = viewYaw;
        viewPitchO = viewPitch;
        float horizontal = (float) Math.sqrt(sx * sx + sz * sz);
        if (sx * sx + sy * sy + sz * sz > 1.0E-8D) {
            viewPitch = (float) (Math.atan2(sy, horizontal) * 180D / Math.PI) - 90F;
        }
        if (horizontal > 1.0E-3F) viewYaw = (float) (Math.atan2(sx, sz) * 180D / Math.PI);
        exhaust(new Vec3(sx, sy, sz));
    }

    protected void exhaust(Vec3 step) {
        plume(step, noseAxis(), 0D, 0D, 0D);
    }

    protected final void plume(Vec3 step, Vec3 nose, double ox, double oy, double oz) {
        MissileExhaustEffects.plume(
                level(),
                getX() + ox,
                getY() + oy,
                getZ() + oz,
                step.x,
                step.y,
                step.z,
                contrailScale(),
                -nose.x,
                -nose.y,
                -nose.z);
    }

    protected Vec3 noseAxis() {
        float pitch = viewPitch * (float) Math.PI / 180F;
        float yaw = (viewYaw + 90F) * (float) Math.PI / 180F;
        double sinPitch = Mth.sin(pitch);
        return new Vec3(sinPitch * Mth.cos(yaw), Mth.cos(pitch), -sinPitch * Mth.sin(yaw));
    }

    protected float contrailScale() {
        return 1F;
    }

    @Override
    public String getRadarName() {
        return missileItem() instanceof MissileItem missile
                ? TIER_NAMES[missile.tier.ordinal()]
                : "Unknown";
    }

    @Override
    public RadarTargetType getTargetType() {
        return missileItem() instanceof MissileItem missile
                ? TIER_BLIPS[missile.tier.ordinal()]
                : RadarTargetType.SPECIAL;
    }

    @Override
    public boolean canBeSeenBy(@Nullable BlockEntity observer) {
        return true;
    }

    @Override
    public boolean matchesScan(RadarScanParams params) {
        return params.scanMissiles();
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return !params.smartMode() || getDeltaMovement().y < 0D;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("sX", startX);
        output.putInt("sZ", startZ);
        output.putInt("tX", targetX);
        output.putInt("tZ", targetZ);
        output.putDouble("veloc", speed);
        output.putDouble("decel", verticalGain);
        output.putDouble("accel", horizontalGain);
        output.putInt("health", health);
        output.putBoolean("cluster", burstsInAir);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        startX = input.getIntOr("sX", startX);
        startZ = input.getIntOr("sZ", startZ);
        targetX = input.getIntOr("tX", targetX);
        targetZ = input.getIntOr("tZ", targetZ);
        speed = input.getDoubleOr("veloc", speed);
        verticalGain = input.getDoubleOr("decel", verticalGain);
        horizontalGain = input.getDoubleOr("accel", horizontalGain);
        health = input.getIntOr("health", health);
        burstsInAir = input.getBooleanOr("cluster", burstsInAir);
    }
}

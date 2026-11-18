/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.entity.projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public abstract class ProjectileEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> ATTACHED_FACE =
            SynchedEntityData.defineId(ProjectileEntity.class, EntityDataSerializers.INT);
    protected boolean inGround;
    public int ticksInGround;
    public int ticksInAir;
    private BlockPos attachedPos = BlockPos.ZERO;
    private Block attachedBlock;
    private List<HitResult> penetrationHits = List.of();
    private Vec3 flightEnd = Vec3.ZERO;

    protected ProjectileEntity(EntityType<? extends ProjectileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ATTACHED_FACE, -1);
    }

    public void initThrower(LivingEntity thrower) {
        initThrower(thrower, 1.5F, 1F);
    }

    protected final void initThrower(LivingEntity thrower, float power, float spread) {
        setOwner(thrower);
        setPos(thrower.getEyePosition().subtract(0, 0.1, 0));
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0, power, spread);
    }

    public final LivingEntity getThrower() {
        return getOwner() instanceof LivingEntity living ? living : null;
    }

    public final void setThrower(LivingEntity thrower) {
        setOwner(thrower);
    }

    public void setThrowableHeading(
            double x, double y, double z, float velocity, float inaccuracy) {
        shoot(x, y, z, velocity, inaccuracy);
    }

    @Override
    public void tick() {
        if (level().isClientSide() && isInterpolated()) {
            super.tick();
            return;
        }
        if (inGround && level().getBlockState(attachedPos).getBlock() != attachedBlock) {
            inGround = false;
            entityData.set(ATTACHED_FACE, -1);
            ticksInGround = 0;
            ticksInAir = 0;
        }
        if (inGround) {
            ++ticksInGround;
            if (groundDespawn() > 0 && ticksInGround >= groundDespawn()) {
                discard();
                return;
            }
        } else {
            ++ticksInAir;
        }
        super.tick();
    }

    public final boolean isFlightPaused() {
        return inGround || level().isClientSide() && isInterpolated();
    }

    protected boolean isInterpolated() {
        return false;
    }

    public void tickInterpolation() {}

    protected final Vec3 flightStart() {
        if (level().isClientSide() && isInterpolated()) return oldPosition();
        return position();
    }

    public boolean isSpectral() {
        return false;
    }

    public boolean doesPenetrate() {
        return false;
    }

    protected int penetrationLimit() {
        return Integer.MAX_VALUE;
    }

    public boolean doesImpactEntities() {
        return true;
    }

    public boolean fullBlockCollisions() {
        return false;
    }

    public int selfDamageDelay() {
        return 0;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return doesImpactEntities()
                && !inGround
                && super.canHitEntity(entity)
                && (entity != getOwner() || tickCount >= selfDamageDelay());
    }

    public final HitResult traceFlight(Predicate<Entity> matching) {
        if (!isSpectral() && !doesPenetrate()) {
            return ProjectileUtil.getHitResultOnMoveVector(this, matching);
        }
        var from = position();
        var movement = getDeltaMovement();
        flightEnd = from.add(movement);
        var blockHit =
                isSpectral()
                        ? BlockHitResult.miss(
                                flightEnd, Direction.UP, BlockPos.containing(flightEnd))
                        : level().clipIncludingBorder(
                                        new ClipContext(
                                                from,
                                                flightEnd,
                                                ClipContext.Block.COLLIDER,
                                                ClipContext.Fluid.NONE,
                                                this));
        var to = blockHit.getType() == HitResult.Type.MISS ? flightEnd : blockHit.getLocation();
        var area = getBoundingBox().expandTowards(movement).inflate(1);
        if (!doesPenetrate()) {
            var entityHit =
                    ProjectileUtil.getEntityHitResult(level(), this, from, to, area, matching);
            return entityHit == null ? blockHit : entityHit;
        }
        var entities = ProjectilePath.entities(this, from, to, matching, isSpectral());
        var hits =
                new ArrayList<HitResult>(
                        entities.subList(0, Math.min(entities.size(), penetrationLimit())));
        if (blockHit.getType() != HitResult.Type.MISS) hits.add(blockHit);
        penetrationHits = hits;
        return hits.isEmpty() ? blockHit : hits.getFirst();
    }

    @Override
    public final ProjectileDeflection hitTargetOrDeflectSelf(HitResult first) {
        if (!doesPenetrate()) return super.hitTargetOrDeflectSelf(first);
        var movement = getDeltaMovement();
        for (var hit : penetrationHits) {
            boolean cancelled = EventHooks.onProjectileImpact(this, hit);
            if (!isAlive() || inGround) return ProjectileDeflection.NONE;
            if (cancelled) continue;
            setPos(hit.getLocation());
            var deflection = super.hitTargetOrDeflectSelf(hit);
            if (!isAlive()
                    || inGround
                    || deflection != ProjectileDeflection.NONE
                    || !getDeltaMovement().equals(movement)
                    || hit.getType() == HitResult.Type.BLOCK) {
                return deflection;
            }
        }
        setPos(flightEnd);
        return ProjectileDeflection.NONE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        onImpact(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        onImpact(result);
    }

    protected abstract void onImpact(HitResult result);

    @Override
    protected double getDefaultGravity() {
        return getGravityVelocity();
    }

    public double getGravityVelocity() {
        return 0.03;
    }

    public float getWaterDrag() {
        return 0.8F;
    }

    protected int groundDespawn() {
        return 1200;
    }

    public final int getStuckIn() {
        return entityData.get(ATTACHED_FACE);
    }

    public final void getStuck(BlockPos pos, int side) {
        attachedPos = pos.immutable();
        attachedBlock = level().getBlockState(pos).getBlock();
        inGround = true;
        entityData.set(ATTACHED_FACE, side);
        setDeltaMovement(Vec3.ZERO);
        sendTeleport();
    }

    public final void sendTeleport() {
        if (level() instanceof ServerLevel server) {
            server.getChunkSource()
                    .chunkMap
                    .sendToTrackingPlayers(this, ClientboundEntityPositionSyncPacket.of(this));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("AirAge", ticksInAir);
        output.putInt("GroundAge", ticksInGround);
        output.putInt("Age", tickCount);
        output.putBoolean("Attached", inGround);
        if (inGround) {
            output.store("AttachedPos", BlockPos.CODEC, attachedPos);
            output.store("AttachedBlock", BuiltInRegistries.BLOCK.byNameCodec(), attachedBlock);
            output.putInt("AttachedFace", getStuckIn());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ticksInAir = input.getIntOr("AirAge", 0);
        ticksInGround = input.getIntOr("GroundAge", 0);
        tickCount = input.getIntOr("Age", 0);
        inGround = input.getBooleanOr("Attached", false);
        if (inGround) {
            attachedPos = input.read("AttachedPos", BlockPos.CODEC).orElseThrow();
            attachedBlock =
                    input.read("AttachedBlock", BuiltInRegistries.BLOCK.byNameCodec())
                            .orElseThrow();
            entityData.set(ATTACHED_FACE, input.getIntOr("AttachedFace", -1));
        }
    }
}

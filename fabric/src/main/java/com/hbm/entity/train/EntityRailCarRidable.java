// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.train;

import com.hbm.entity.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class EntityRailCarRidable extends EntityRailCarCargo {

    public double engineSpeed;
    public SeatDummyEntity[] passengerSeats;

    public EntityRailCarRidable(EntityType<? extends EntityRailCarRidable> type, Level level) {
        super(type, level);
        this.passengerSeats = new SeatDummyEntity[this.getPassengerSeats().length];
    }

    public abstract double getPoweredAcceleration();

    public abstract double getPassivBrake();

    public abstract boolean shouldUseEngineBrake(Player player);

    public abstract double getMaxPoweredSpeed();

    public abstract boolean canAccelerate();

    public void consumeFuel() {}

    public double getGravitySpeed() {
        return 0D;
    }

    @Override
    public double getCurrentSpeed() {

        if (this.getFirstPassenger() instanceof Player player) {

            int forward = 0;
            if (player instanceof ServerPlayer server) {
                boolean f = server.getLastClientInput().forward();
                boolean b = server.getLastClientInput().backward();
                forward = f == b ? 0 : f ? 1 : -1;
            }

            if (this.canAccelerate()) {
                if (forward > 0) {
                    engineSpeed += this.getPoweredAcceleration();
                    this.consumeFuel();
                } else if (forward < 0) {
                    engineSpeed -= this.getPoweredAcceleration();
                    this.consumeFuel();
                } else {
                    if (this.shouldUseEngineBrake(player)) {
                        engineSpeed *= this.getPassivBrake();
                    } else {
                        this.consumeFuel();
                    }
                }
            } else {
                engineSpeed *= this.getPassivBrake();
            }

        } else {
            engineSpeed *= this.getPassivBrake();
        }

        double maxSpeed = this.getMaxPoweredSpeed();
        engineSpeed = Mth.clamp(engineSpeed, -maxSpeed, maxSpeed);

        return engineSpeed + this.getGravitySpeed();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {

        InteractionResult coupled = super.interact(player, hand, location);
        if (coupled != InteractionResult.PASS) return coupled;
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        int nearestSeat = this.getNearestSeat(player);

        if (nearestSeat == -1) {
            player.startRiding(this);
        } else if (nearestSeat >= 0) {
            SeatDummyEntity dummySeat =
                    new SeatDummyEntity(ModEntities.SEAT_DUMMY.get(), level(), this, nearestSeat);
            Vec3 seat = this.getPassengerSeats()[nearestSeat];
            rotate(seat, 0F, this.getYRot());
            dummySeat.setPos(renderX + rotatedX, renderY + rotatedY - 1, renderZ + rotatedZ);
            passengerSeats[nearestSeat] = dummySeat;
            level().addFreshEntity(dummySeat);
            player.startRiding(dummySeat);
        }

        return InteractionResult.SUCCESS;
    }

    public int getNearestSeat(Player player) {

        double nearestDist = Double.POSITIVE_INFINITY;
        int nearestSeat = -3;

        Vec3[] seats = getPassengerSeats();
        Vec3 look = player.getEyePosition().add(player.getViewVector(1F));

        for (int i = 0; i < seats.length; i++) {

            Vec3 seat = seats[i];
            if (seat == null) continue;
            if (passengerSeats[i] != null) continue;

            rotate(seat, 0F, this.getYRot());
            double dist =
                    look.distanceTo(
                            new Vec3(renderX + rotatedX, renderY + rotatedY, renderZ + rotatedZ));

            if (dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = i;
            }
        }

        if (this.getFirstPassenger() == null) {
            rotate(getRiderSeatPosition(), 0F, this.getYRot());
            double dist =
                    look.distanceTo(
                            new Vec3(renderX + rotatedX, renderY + rotatedY, renderZ + rotatedZ));

            if (dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = -1;
            }
        }

        if (nearestDist > 180) return -2;

        return nearestSeat;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {

            Vec3[] seats = this.getPassengerSeats();
            for (int i = 0; i < passengerSeats.length; i++) {
                SeatDummyEntity seat = passengerSeats[i];

                if (seat != null) {
                    if (seat.getFirstPassenger() == null) {
                        passengerSeats[i] = null;
                        seat.discard();
                    } else {
                        rotate(seats[i], this.getXRot(), this.getYRot());
                        seat.setPos(renderX + rotatedX, renderY + rotatedY - 1, renderZ + rotatedZ);
                    }
                }
            }
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        rotate(getRiderSeatPosition(), this.getXRot(), this.getYRot());
        moveFunction.accept(passenger, renderX + rotatedX, renderY + rotatedY, renderZ + rotatedZ);
    }

    protected double rotatedX, rotatedY, rotatedZ;

    protected void rotate(Vec3 offset, float pitchDeg, float yawDeg) {
        float pitch = (float) (pitchDeg * Math.PI / 180D);
        float yaw = (float) (-yawDeg * Math.PI / 180D);
        float cp = Mth.cos(pitch), sp = Mth.sin(pitch);
        float cy = Mth.cos(yaw), sy = Mth.sin(yaw);

        double py = offset.y * cp + offset.z * sp;
        double pz = offset.z * cp - offset.y * sp;

        this.rotatedX = offset.x * cy + pz * sy;
        this.rotatedY = py;
        this.rotatedZ = pz * cy - offset.x * sy;
    }

    public abstract Vec3 getRiderSeatPosition();

    public abstract Vec3[] getPassengerSeats();

    public static class SeatDummyEntity extends Entity {

        private static final EntityDataAccessor<Integer> TRAIN_ID =
                SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Integer> SEAT_INDEX =
                SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);

        public EntityRailCarRidable train;

        public SeatDummyEntity(EntityType<? extends SeatDummyEntity> type, Level level) {
            super(type, level);
        }

        public SeatDummyEntity(
                EntityType<? extends SeatDummyEntity> type,
                Level level,
                EntityRailCarRidable train,
                int index) {
            this(type, level);
            this.train = train;
            if (train != null) this.entityData.set(TRAIN_ID, train.getId());
            this.entityData.set(SEAT_INDEX, index);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            builder.define(TRAIN_ID, 0);
            builder.define(SEAT_INDEX, 0);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(0.5F, 0.1F);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        protected void readAdditionalSaveData(ValueInput input) {

            this.discard();
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {}

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
            return false;
        }

        @Override
        public void tick() {
            if (!level().isClientSide()) {
                if (this.train == null || this.train.isRemoved()) this.discard();
            }
        }

        @Override
        protected void positionRider(Entity passenger, MoveFunction moveFunction) {

            if (train == null
                    && level().getEntity(this.entityData.get(TRAIN_ID))
                            instanceof EntityRailCarRidable car) {
                train = car;
            }

            if (train == null) {
                moveFunction.accept(passenger, getX(), getY() + 1, getZ());
                return;
            }

            Vec3[] seats = train.getPassengerSeats();
            int index = this.entityData.get(SEAT_INDEX);
            if (index < 0 || index >= seats.length) {
                moveFunction.accept(passenger, getX(), getY() + 1, getZ());
                return;
            }

            train.rotate(seats[index], train.getXRot(), train.getYRot());
            moveFunction.accept(
                    passenger,
                    train.renderX + train.rotatedX,
                    train.renderY + train.rotatedY,
                    train.renderZ + train.rotatedZ);
        }
    }
}

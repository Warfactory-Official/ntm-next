// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.train;

import com.hbm.blocks.rail.IRailNTM.TrackGauge;
import com.hbm.inventory.container.MenuTrainCargoTram;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TrainCargoTram extends EntityRailCarElectric {

    public static final int SLOT_COUNT = 29;
    public static final int SLOT_CHARGE = 28;

    private static final DummyConfig[] DUMMIES = {
        new DummyConfig(2F, 1F, 0, 0, 1.5),
        new DummyConfig(2F, 1F, 0, 0, 0),
        new DummyConfig(2F, 1F, 0, 0, -1.5)
    };
    private static final Vec3 RIDER_SEAT = new Vec3(0.375, 2.375, 0.5);
    private static final Vec3[] SEATS = {new Vec3(0.5, 1.75, -1.5), new Vec3(-0.5, 1.75, -1.5)};

    public TrainCargoTram(EntityType<? extends TrainCargoTram> type, Level level) {
        super(type, level);
    }

    @Override
    public double getPoweredAcceleration() {
        return 0.01;
    }

    @Override
    public double getPassivBrake() {
        return 0.95;
    }

    @Override
    public boolean shouldUseEngineBrake(Player player) {
        return Math.abs(this.engineSpeed) < 0.1;
    }

    @Override
    public double getMaxPoweredSpeed() {
        return 0.5;
    }

    @Override
    public double getMaxRailSpeed() {
        return 1;
    }

    @Override
    public TrackGauge getGauge() {
        return TrackGauge.STANDARD;
    }

    @Override
    public double getLengthSpan() {
        return 1.5;
    }

    @Override
    public double getCollisionSpan() {
        return 2.5;
    }

    @Override
    public Vec3 getRiderSeatPosition() {
        return RIDER_SEAT;
    }

    @Override
    public Vec3[] getPassengerSeats() {
        return SEATS;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public DummyConfig[] getDummies() {
        return DUMMIES;
    }

    @Override
    public double getCouplingDist(TrainCoupling coupling) {
        return coupling != null ? 2.75 : 0;
    }

    @Override
    public int getMaxPower() {
        return this.getPowerConsumption() * 100;
    }

    @Override
    public int getPowerConsumption() {
        return 10;
    }

    @Override
    public boolean hasChargeSlot() {
        return true;
    }

    @Override
    public int getChargeSlot() {
        return SLOT_CHARGE;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.isRemoved()) this.discard();
        return true;
    }

    @Override
    public Component getDisplayName() {
        return this.hasCustomName()
                ? this.getCustomName()
                : Component.translatable("container.trainTram");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new MenuTrainCargoTram(containerId, playerInv, this);
    }
}

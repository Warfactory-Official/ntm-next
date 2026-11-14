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

package com.hbm.entity.train;

import com.hbm.blocks.rail.RailGauge;
import com.hbm.inventory.container.TrainCargoTramMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TrainCargoTram extends RailCarElectricEntity {

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
    public RailGauge getGauge() {
        return RailGauge.STANDARD;
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
        return new TrainCargoTramMenu(containerId, playerInv, this);
    }
}

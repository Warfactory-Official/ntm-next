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

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DeuteriumExtractorBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidTankAccess,
                FluidSettingsHandler,
                SyncUnitSchema {

    public static final long ENERGY_CAPACITY = 10_000L;
    public static final int INPUT_PER_OUTPUT = 50;
    public static final int REQUIRED_INPUT = 100;

    public long energy;
    public final FluidTankNTM feed;
    public final FluidTankNTM product;

    private final FluidTankNTM[] tanks;
    private final FluidTankNTM[] intake;
    private final FluidTankNTM[] delivery;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public DeuteriumExtractorBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.DEUTERIUM_EXTRACTOR.get(), pos, state, 1_000, 100);
    }

    protected DeuteriumExtractorBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int inputCapacity,
            int outputCapacity) {
        super(type, pos, state, 0);
        feed = new FluidTankNTM(NTMFluids.WATER, inputCapacity);
        product = new FluidTankNTM(NTMFluids.HEAVYWATER, outputCapacity);
        tanks = new FluidTankNTM[] {feed, product};
        intake = new FluidTankNTM[] {feed};
        delivery = new FluidTankNTM[] {product};
    }

    protected boolean energyReady() {
        return energy >= getMaxPower() / 20L;
    }

    @Override
    public void tickServer() {
        if (energyReady()
                && feed.getFill() >= REQUIRED_INPUT
                && product.getFill() < product.getMaxFill()) {
            int amount =
                    Math.min(
                            Math.min(product.getMaxFill(), feed.getFill()) / INPUT_PER_OUTPUT,
                            product.getMaxFill() - product.getFill());
            feed.setFill(feed.getFill() - amount * INPUT_PER_OUTPUT);
            product.setFill(product.getFill() + amount);
            setPower(energy - getMaxPower() / 20L);
        }
        flush.provide((ServerLevel) level, this);
        networkPackNT(50);
    }

    @Override
    public long getPower() {
        return energy;
    }

    @Override
    public void setPower(long stored) {
        long next = Math.clamp(stored, 0L, getMaxPower());
        if (next != energy) {
            hbm$syncSet$DeuteriumExtractorBlockEntity$energy(next);
            markChanged();
        }
    }

    @Override
    public long getMaxPower() {
        return ENERGY_CAPACITY;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return intake;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return delivery;
    }

    @Override
    public void declareFlush(FlushLanes lanes) {
        lanes.add(product, FlushFaces.own());
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    protected void loadAdditional(ValueInput data) {
        super.loadAdditional(data);
        hbm$syncSet$DeuteriumExtractorBlockEntity$energy(data.getLongOr("power", 0L));
        data.child("water").ifPresent(feed::deserialize);
        data.child("heavyWater").ifPresent(product::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        data.putLong("power", energy);
        feed.serialize(data.child("water"));
        product.serialize(data.child("heavyWater"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf bytes) {
        switch (unit) {
            case 0 -> bytes.writeLong(energy);
            case 1 -> {
                feed.packetSerialize(bytes);
                product.packetSerialize(bytes);
            }
            default -> super.writeSyncUnit(unit, bytes);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf bytes) {
        switch (unit) {
            case 0 -> hbm$syncSet$DeuteriumExtractorBlockEntity$energy(bytes.readLong());
            case 1 -> {
                feed.packetDeserialize(bytes);
                product.packetDeserialize(bytes);
            }
            default -> super.readSyncUnit(unit, bytes);
        }
    }

    public final void hbm$syncSet$DeuteriumExtractorBlockEntity$energy(long next) {
        if (this.energy != next) {
            this.energy = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(DeuteriumExtractorBlockEntity.class, "feed", 3, 1L << 1);
        com.hbm.packet.SyncBindings.field(
                DeuteriumExtractorBlockEntity.class, "product", 3, 1L << 1);
    }
}

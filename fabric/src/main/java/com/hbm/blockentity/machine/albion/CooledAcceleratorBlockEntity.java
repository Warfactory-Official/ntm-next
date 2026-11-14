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

package com.hbm.blockentity.machine.albion;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.PAState;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.Particle;
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
import org.jspecify.annotations.Nullable;

public abstract class CooledAcceleratorBlockEntity extends MachineBlockEntity
        implements EnergyHandler, FluidTankHandler, FluidTankAccess, SyncUnitSchema {

    public static final float KELVIN = 273F;
    public static final float TEMPERATURE_TARGET = 123F;
    public static final float TEMP_CHANGE_PER_MB = 0.5F;
    public static final float TEMP_PASSIVE_HEATING = 2.5F;
    public static final float TEMP_CHANGE_MAX = 7.5F;

    private static final float AMBIENT_TEMPERATURE = 293F;
    private static final FlushFaces OUTPUT_FACES = FlushFaces.own();

    public final FluidTankNTM[] coolantTanks;
    public float temperature;

    {
        hbm$syncSet$CooledAcceleratorBlockEntity$temperature(AMBIENT_TEMPERATURE);
    }

    public long power;

    protected final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] coolantIntake;
    private final FluidTankNTM[] coolantOutput;

    protected CooledAcceleratorBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
        coolantTanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(NTMFluids.PERFLUOROMETHYL_COLD, 4_000),
                    new FluidTankNTM(NTMFluids.PERFLUOROMETHYL, 4_000)
                };
        coolantIntake = new FluidTankNTM[] {coolantTanks[0]};
        coolantOutput = new FluidTankNTM[] {coolantTanks[1]};
    }

    @Override
    public void tickServer() {
        flush.provide((ServerLevel) level, this);
        tickCooling();
        networkPackNT(50);
    }

    protected void tickCooling() {
        float previousTemperature = temperature;
        float warmedTemperature =
                Math.min(AMBIENT_TEMPERATURE, previousTemperature + TEMP_PASSIVE_HEATING);
        int converted = 0;
        if (warmedTemperature > TEMPERATURE_TARGET) {
            float cooling = Math.min(warmedTemperature - TEMPERATURE_TARGET, TEMP_CHANGE_MAX);
            int requested = (int) Math.ceil(cooling / TEMP_CHANGE_PER_MB);
            var cold = coolantTanks[0];
            var spent = coolantTanks[1];
            int room = spent.getMaxFill() - spent.getFill();
            converted = Math.min(requested, Math.min(cold.getFill(), room));
            if (converted > 0) {
                cold.setFill(cold.getFill() - converted);
                spent.setFill(spent.getFill() + converted);
            }
        }
        hbm$syncSet$CooledAcceleratorBlockEntity$temperature(
                warmedTemperature - converted * TEMP_CHANGE_PER_MB);
        if (temperature != previousTemperature || converted != 0) setChanged();
    }

    public boolean isCool() {
        return temperature <= TEMPERATURE_TARGET;
    }

    protected final boolean admit(Particle particle, long cost, @Nullable PAState fault) {
        if (!isCool()) particle.crash(PAState.CRASH_NOCOOL);
        if (power < cost) particle.crash(PAState.CRASH_NOPOWER);
        if (fault != null) particle.crash(fault);
        if (particle.invalid) return false;
        if (cost != 0) {
            hbm$syncSet$CooledAcceleratorBlockEntity$power((long) (power - (cost)));
            setChanged();
        }
        return true;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.hbm$syncSet$CooledAcceleratorBlockEntity$power(power);
    }

    @Override
    public int tankCount() {
        return coolantTanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return coolantTanks[index];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return coolantIntake;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return coolantOutput;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (var tank : getSendingTanks()) out.add(tank, OUTPUT_FACES);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(coolantTanks[0]::deserialize);
        input.child("t1").ifPresent(coolantTanks[1]::deserialize);
        hbm$syncSet$CooledAcceleratorBlockEntity$temperature(
                input.getFloatOr("temperature", AMBIENT_TEMPERATURE));
        hbm$syncSet$CooledAcceleratorBlockEntity$power(input.getLongOr("power", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        coolantTanks[0].serialize(output.child("t0"));
        coolantTanks[1].serialize(output.child("t1"));
        output.putFloat("temperature", temperature);
        output.putLong("power", power);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                coolantTanks[0].packetSerialize(output);
                coolantTanks[1].packetSerialize(output);
            }
            case 1 -> output.writeFloat(temperature);
            case 2 -> output.writeLong(power);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                coolantTanks[0].packetDeserialize(input);
                coolantTanks[1].packetDeserialize(input);
            }
            case 1 -> hbm$syncSet$CooledAcceleratorBlockEntity$temperature(input.readFloat());
            case 2 -> hbm$syncSet$CooledAcceleratorBlockEntity$power(input.readLong());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$CooledAcceleratorBlockEntity$coolantTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.coolantTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.coolantTanks) {
                syncArrayChanged(array, index, 3, 1L);
            }
        }
        return next;
    }

    public final void hbm$syncSet$CooledAcceleratorBlockEntity$temperature(float next) {
        if (Float.floatToRawIntBits(this.temperature) != Float.floatToRawIntBits(next)) {
            this.temperature = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$CooledAcceleratorBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                CooledAcceleratorBlockEntity.class, "coolantTanks", 3, 1L);
    }
}

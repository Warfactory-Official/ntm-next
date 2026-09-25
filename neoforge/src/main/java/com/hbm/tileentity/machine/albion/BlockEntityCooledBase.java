// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.albion;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class BlockEntityCooledBase extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, FluidTankEndpoint, SyncUnitSchema {

    public static final float KELVIN = 273F;
    public static final float TEMPERATURE_TARGET = KELVIN - 150F;
    public static final float TEMP_CHANGE_PER_MB = 0.5F;
    public static final float TEMP_PASSIVE_HEATING = 2.5F;
    public static final float TEMP_CHANGE_MAX = 5F + TEMP_PASSIVE_HEATING;

    @SyncField(units = 1L << 0)
    public final FluidTankNTM[] coolantTanks;

    @SyncField(units = 1L << 2)
    public long power;

    @SyncField(units = 1L << 1)
    public float temperature = KELVIN + 20;

    protected final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    protected BlockEntityCooledBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
        coolantTanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(NTMFluids.PERFLUOROMETHYL_COLD, 4_000),
                    new FluidTankNTM(NTMFluids.PERFLUOROMETHYL, 4_000)
                };
        receiving = new FluidTankNTM[] {coolantTanks[0]};
        sending = new FluidTankNTM[] {coolantTanks[1]};
    }

    @Override
    public void tickServer() {

        flush.provide((ServerLevel) level, this);

        tickCooling();
        networkPackNT(50);
    }

    protected void tickCooling() {
        temperature += TEMP_PASSIVE_HEATING;
        if (temperature > KELVIN + 20) temperature = KELVIN + 20;

        if (temperature <= TEMPERATURE_TARGET) return;

        int cyclesTemp =
                (int)
                        Math.ceil(
                                Math.min(temperature - TEMPERATURE_TARGET, TEMP_CHANGE_MAX)
                                        / TEMP_CHANGE_PER_MB);
        int cyclesCool = coolantTanks[0].getFill();
        int cyclesHot = coolantTanks[1].getMaxFill() - coolantTanks[1].getFill();
        int cycles = Math.min(cyclesTemp, Math.min(cyclesCool, cyclesHot));

        coolantTanks[0].setFill(coolantTanks[0].getFill() - cycles);
        coolantTanks[1].setFill(coolantTanks[1].getFill() + cycles);
        temperature -= TEMP_CHANGE_PER_MB * cycles;
    }

    public boolean isCool() {
        return temperature <= TEMPERATURE_TARGET;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(coolantTanks[0]::deserialize);
        input.child("t1").ifPresent(coolantTanks[1]::deserialize);
        temperature = input.getFloatOr("temperature", KELVIN + 20);
        power = input.getLongOr("power", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        coolantTanks[0].serialize(output.child("t0"));
        coolantTanks[1].serialize(output.child("t1"));
        output.putFloat("temperature", temperature);
        output.putLong("power", power);
    }

    private void writeCoolantTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) coolantTanks[i].packetSerialize(output);
    }

    private void readCoolantTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) coolantTanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeCoolantTanks(output);
            case 1 -> output.writeFloat(this.temperature);
            case 2 -> output.writeLong(this.power);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readCoolantTanks(input);
            case 1 -> this.temperature = input.readFloat();
            case 2 -> this.power = input.readLong();
            default -> super.readSyncUnit(unit, input);
        }
    }
}

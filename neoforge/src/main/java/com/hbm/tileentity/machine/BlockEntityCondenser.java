// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityCondenser extends BlockEntityMachineBase
        implements FluidFlushSender, IFluidCopiable, SyncUnitSchema {

    @SyncField(units = 1L << 0)
    public final FluidTankNTM input;

    @SyncField(units = 1L << 1)
    public final FluidTankNTM output;

    @SyncField(units = 1L << 2)
    public int waterTimer;

    public int age;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntityCondenser(BlockPos pos, BlockState state) {
        this(
                ModBlockEntities.CONDENSER.get(),
                pos,
                state,
                MachineData.CONDENSER_INPUT_TANK_SIZE.get(),
                MachineData.CONDENSER_OUTPUT_TANK_SIZE.get());
    }

    protected BlockEntityCondenser(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int inCap, int outCap) {
        super(type, pos, state, 0);
        this.input = new FluidTankNTM(NTMFluids.SPENTSTEAM, inCap);
        this.output = new FluidTankNTM(NTMFluids.WATER, outCap);
        sending = new FluidTankNTM[] {output};
    }

    @Override
    public void tickServer() {
        age++;
        if (age >= 2) age = 0;

        if (waterTimer > 0) waterTimer--;

        int convert = Math.min(input.getFill(), output.getMaxFill() - output.getFill());

        if (extraCondition(convert)) {
            input.setFill(input.getFill() - convert);
            if (convert > 0) waterTimer = 20;
            output.setFill(output.getFill() + convert);
            postConvert(convert);
            if (convert > 0) setChanged();
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    protected boolean extraCondition(int convert) {
        return true;
    }

    protected void postConvert(int convert) {}

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != input.getPressure() || !input.accepts(type)) return 0L;
        return (long) input.getMaxFill() - input.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != input.getPressure() || !input.accepts(type)) return amount;
        int accepted = input.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (!output.provides(type) || pressure != output.getPressure()) return 0L;
        return output.getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!output.provides(type) || pressure != output.getPressure()) return;
        if (output.drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {input, output};
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("water").ifPresent(input::deserialize);
        in.child("steam").ifPresent(output::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        input.serialize(out.child("water"));
        output.serialize(out.child("steam"));
    }

    private void writeWaterTimer(ByteBuf output) {
        output.writeByte(waterTimer);
    }

    private void readWaterTimer(ByteBuf input) {
        waterTimer = input.readByte();
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.input.packetSerialize(output);
            case 1 -> this.output.packetSerialize(output);
            case 2 -> writeWaterTimer(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.input.packetDeserialize(input);
            case 1 -> this.output.packetDeserialize(input);
            case 2 -> readWaterTimer(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
